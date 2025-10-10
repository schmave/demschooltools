import React, {
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react';
import {
  Box,
  Button,
  Card,
  CardContent,
  CardHeader,
  Paper,
  Stack,
  Typography,
  SelectInput,
} from '../../components';
import { DeleteDialog } from '../../containers';
import { SnackbarContext } from '../../contexts';
import { safeParse, normalizeOption, buildOptionMap } from '../../utils';
import CaseCard from './CaseCard';
import { TIME_SERVED_LABEL } from './constants';

const SAVE_DEBOUNCE_MS = 1200;
const defaultSidebarContent = `
  <div>
    <p>Click a person's name to view their RC history.</p>
    <p>Click a rule title to explore related charges.</p>
    <p>Use the “More info” links for combined person + rule context.</p>
  </div>
`;

const createEmptyCharge = (chargeId) => ({
  id: chargeId,
  resolutionPlan: '',
  plea: '',
  severity: '',
  referredToSm: false,
  minorReferralDestination: '',
  person: null,
  rule: null,
  lastResolutionHtml: '',
  followUpChoice: 'original',
  referencedSource: null,
  referencedStatus: {
    hasGenerated: false,
  },
  isReferenced: false,
});


const EditMinutesPage = () => {
  const { setSnackbar } = useContext(SnackbarContext);

  const handleError = useCallback(
    (message, error) => {
      console.error(message, error);
      setSnackbar({ message, severity: 'error' });
    },
    [setSnackbar],
  );

  const post = useCallback(
    async (url, { body, headers, errorMessage } = {}) => {
      try {
        const response = await fetch(url, {
          method: 'POST',
          body,
          headers,
        });
        if (!response.ok) {
          throw new Error(`HTTP ${response.status}`);
        }
        return response;
      } catch (error) {
        handleError(errorMessage || 'Request failed', error);
        throw error;
      }
    },
    [handleError],
  );

  const get = useCallback(
    async (url, { errorMessage } = {}) => {
      try {
        const response = await fetch(url, {
          method: 'GET',
          headers: {
            'X-Requested-With': 'XMLHttpRequest',
          },
        });
        if (!response.ok) {
          throw new Error(`HTTP ${response.status}`);
        }
        return response;
      } catch (error) {
        handleError(errorMessage || 'Request failed', error);
        throw error;
      }
    },
    [handleError],
  );

  const meetingId = window.initialData?.meeting_id;

  const initialPeople = useMemo(
    () => safeParse(window.initialData?.people, []),
    [],
  );
  const peopleOptions = useMemo(
    () =>
      initialPeople
        .map((person) => normalizeOption(person))
        .filter(Boolean),
    [initialPeople],
  );
  const peopleOptionMap = useMemo(
    () => buildOptionMap(peopleOptions),
    [peopleOptions],
  );

  const ruleOptions = useMemo(() => {
    const parsedRules = safeParse(window.initialData?.rules, []);
    return parsedRules
      .map((rule) => normalizeOption(rule))
      .filter(Boolean);
  }, []);
  const ruleOptionMap = useMemo(
    () => buildOptionMap(ruleOptions),
    [ruleOptions],
  );

  const caseOptions = useMemo(() => {
    const parsedCases = safeParse(window.initialData?.cases, []);
    return parsedCases
      .map((entry) => normalizeOption(entry))
      .filter(Boolean);
  }, []);

  const roleIds = useMemo(
    () => ({
      chair: window.initialData?.ROLE_JC_CHAIR,
      committee: window.initialData?.ROLE_JC_MEMBER,
      noteTaker: window.initialData?.ROLE_NOTE_TAKER,
      sub: window.initialData?.ROLE_JC_SUB,
      runner: window.initialData?.ROLE_RUNNER,
      testifier: window.initialData?.ROLE_TESTIFIER,
      writer: window.initialData?.ROLE_WRITER,
    }),
    [],
  );

  const config = useMemo(
    () => safeParse(window.initialData?.config, {}),
    [],
  );

  const messages = useMemo(
    () => safeParse(window.initialData?.messages, {}),
    [],
  );

  const mapPersonOption = useCallback(
    (person) => {
      if (!person) {
        return null;
      }

      if (person.personId !== undefined && person.personId !== null) {
        const key = String(person.personId);
        if (peopleOptionMap.has(key)) {
          return peopleOptionMap.get(key);
        }
        const label =
          person.displayName ||
          [person.firstName, person.lastName].filter(Boolean).join(' ') ||
          '';
        return {
          id: key,
          label,
          personId: person.personId,
        };
      }

      if (person.id !== undefined && person.id !== null) {
        const key = String(person.id);
        if (peopleOptionMap.has(key)) {
          return peopleOptionMap.get(key);
        }
      }

      return normalizeOption(person);
    },
    [peopleOptionMap],
  );

  const mapRuleOption = useCallback(
    (rule) => {
      if (!rule) {
        return null;
      }
      const rawId = rule.id ?? rule.rule_id;
      if (rawId === undefined || rawId === null) {
        return null;
      }
      const key = String(rawId);
      if (ruleOptionMap.has(key)) {
        return ruleOptionMap.get(key);
      }
      const label =
        rule.label ||
        `${rule.number || rule.num || ''} ${rule.title || ''}`.trim();
      return {
        id: key,
        label,
      };
    },
    [ruleOptionMap],
  );

  const mapChargeFromServer = useCallback(
    (charge) => {
      if (!charge) {
        return null;
      }
      const chargeId = charge.id !== undefined ? Number(charge.id) : undefined;
      return {
        id: chargeId,
        resolutionPlan: charge.resolutionPlan || '',
        plea:
          charge.plea && charge.plea !== '<no plea>' ? charge.plea : '',
        severity: charge.severity || '',
        referredToSm: Boolean(charge.referredToSm),
        minorReferralDestination: charge.minorReferralDestination || '',
        person: mapPersonOption(charge.person),
        rule: mapRuleOption(charge.rule),
        lastResolutionHtml: '',
        followUpChoice:
          charge.resolutionPlan === TIME_SERVED_LABEL ? 'timeServed' : 'original',
        referencedSource: null,
        referencedStatus: {
          hasGenerated: false,
        },
        isReferenced: Boolean(charge.isReferenced),
      };
    },
    [mapPersonOption, mapRuleOption],
  );

  const mapCaseFromServer = useCallback(
    (caseData) => {
      if (!caseData) {
        return null;
      }

      const testifierRole = Number(roleIds.testifier);
      const writerRole = Number(roleIds.writer);

      const peopleAtCase = Array.isArray(caseData.people_at_case)
        ? caseData.people_at_case
        : [];

      const testifiers = [];
      const writers = [];

      peopleAtCase.forEach((pac) => {
        const option = mapPersonOption(pac.person);
        if (!option) {
          return;
        }
        if (pac.role === testifierRole) {
          testifiers.push(option);
        } else if (pac.role === writerRole) {
          writers.push(option);
        }
      });

      return {
        id: Number(caseData.id),
        caseNumber: caseData.caseNumber,
        location: caseData.location || '',
        findings: caseData.findings || '',
        date: caseData.date || '',
        time: caseData.time || '',
        continued: caseData.dateClosed === null,
        charges: Array.isArray(caseData.charges)
          ? caseData.charges
              .map((charge) => mapChargeFromServer(charge))
              .filter(Boolean)
          : [],
        testifiers,
        writers,
        caseReferences: [],
        referencesLoading: false,
        referencesLoaded: false,
      };
    },
    [mapChargeFromServer, mapPersonOption, roleIds.testifier, roleIds.writer],
  );

  const [committee, setCommittee] = useState(
    safeParse(window.initialData?.committee, [])
      .map((person) => normalizeOption(person))
      .filter(Boolean),
  );
  const [chair, setChair] = useState(
    safeParse(window.initialData?.chair, [])
      .map((person) => normalizeOption(person))
      .filter(Boolean),
  );
  const [noteTaker, setNoteTaker] = useState(
    safeParse(window.initialData?.notetaker, [])
      .map((person) => normalizeOption(person))
      .filter(Boolean),
  );
  const [subs, setSubs] = useState(
    safeParse(window.initialData?.sub, [])
      .map((person) => normalizeOption(person))
      .filter(Boolean),
  );
  const [runners, setRunners] = useState(
    safeParse(window.initialData?.runners, [])
      .map((person) => normalizeOption(person))
      .filter(Boolean),
  );

  const [meetingCases, setMeetingCases] = useState(() => {
    const parsedCases = safeParse(window.initialData?.meetingCases, []);
    if (!Array.isArray(parsedCases)) {
      return [];
    }
    return parsedCases
      .map((caseData) => mapCaseFromServer(caseData))
      .filter(Boolean);
  });

  const [openCases, setOpenCases] = useState(() => {
    const parsedOpenCases = safeParse(window.initialData?.openCases, []);
    return Array.isArray(parsedOpenCases)
      ? parsedOpenCases.map((caseData) => ({
          ...caseData,
          id: caseData?.id,
          caseNumber: caseData?.caseNumber,
        }))
      : [];
  });

  const visibleOpenCases = useMemo(() => {
    const loadedIds = new Set(meetingCases.map((caseItem) => caseItem.id));
    return openCases.filter((openCase) => !loadedIds.has(openCase.id));
  }, [meetingCases, openCases]);

  const [sidebarContent, setSidebarContent] = useState(defaultSidebarContent);
  const [sidebarLoading, setSidebarLoading] = useState(false);
  const [pendingSaveCount, setPendingSaveCount] = useState(0);
  const [caseToConfirmClear, setCaseToConfirmClear] = useState(null);

  const pendingSaveKeysRef = useRef(new Set());
  const caseSaveTimersRef = useRef(new Map());
  const chargeSaveTimersRef = useRef(new Map());
  const casesRef = useRef([]);

  useEffect(() => {
    casesRef.current = meetingCases;
  }, [meetingCases]);

  useEffect(() => {
    return () => {
      caseSaveTimersRef.current.forEach((timeoutId) => {
        window.clearTimeout(timeoutId);
      });
      chargeSaveTimersRef.current.forEach((timeoutId) => {
        window.clearTimeout(timeoutId);
      });
    };
  }, []);

  const registerPendingSave = useCallback((key) => {
    const setRef = pendingSaveKeysRef.current;
    if (!setRef.has(key)) {
      setRef.add(key);
      setPendingSaveCount(setRef.size);
    }
  }, []);

  const completePendingSave = useCallback((key) => {
    const setRef = pendingSaveKeysRef.current;
    if (setRef.delete(key)) {
      setPendingSaveCount(setRef.size);
    }
  }, []);

  const saveCase = useCallback(
    async (caseItem) => {
      if (!caseItem?.id) {
        return;
      }
      const formData = new URLSearchParams();
      formData.append('closed', (!caseItem.continued).toString());
      formData.append('date', caseItem.date || '');
      formData.append('findings', caseItem.findings || '');
      formData.append('location', caseItem.location || '');
      formData.append('time', caseItem.time || '');
      await post(`/saveCase?id=${caseItem.id}`, {
        body: formData,
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        errorMessage: 'Unable to save case',
      });
    },
    [post],
  );

  const findChargeById = useCallback((chargeId) => {
    const currentCases = casesRef.current;
    for (let i = 0; i < currentCases.length; i += 1) {
      const charge = currentCases[i].charges.find((item) => item.id === chargeId);
      if (charge) {
        return { caseItem: currentCases[i], charge };
      }
    }
    return { caseItem: null, charge: null };
  }, []);

  const saveCharge = useCallback(
    async (charge) => {
      if (!charge?.id) {
        return;
      }

      const params = new URLSearchParams();

      if (charge.person?.id) {
        params.append('personId', String(charge.person.id));
      }
      params.append('resolutionPlan', charge.resolutionPlan || '');

      if (charge.severity) {
        params.append('severity', charge.severity);
      }

      if (charge.plea) {
        params.append('plea', charge.plea);
      }

      params.append('referredToSm', charge.referredToSm ? 'true' : 'false');

      if (config.use_minor_referrals) {
        params.append(
          'minorReferralDestination',
          charge.minorReferralDestination || '',
        );
      }

      if (charge.rule?.id) {
        params.append('rule_id', String(charge.rule.id));
      }

      const query = params.toString();
      const url = query
        ? `/saveCharge?id=${charge.id}&${query}`
        : `/saveCharge?id=${charge.id}`;

      await post(url, {
        errorMessage: 'Unable to save charge',
      });
    },
    [config.use_minor_referrals, post],
  );

  const queueCaseSave = useCallback(
    (caseId) => {
      if (!caseId) {
        return;
      }
      const key = `case-${caseId}`;
      registerPendingSave(key);

      if (caseSaveTimersRef.current.has(caseId)) {
        window.clearTimeout(caseSaveTimersRef.current.get(caseId));
      }

      const timeoutId = window.setTimeout(async () => {
        caseSaveTimersRef.current.delete(caseId);
        try {
          const currentCases = casesRef.current;
          const caseItem = currentCases.find((item) => item.id === caseId);
          if (caseItem) {
            await saveCase(caseItem);
          }
        } finally {
          completePendingSave(key);
        }
      }, SAVE_DEBOUNCE_MS);

      caseSaveTimersRef.current.set(caseId, timeoutId);
    },
    [completePendingSave, registerPendingSave, saveCase],
  );

  const queueChargeSave = useCallback(
    (chargeId) => {
      if (!chargeId) {
        return;
      }
      const key = `charge-${chargeId}`;
      registerPendingSave(key);

      if (chargeSaveTimersRef.current.has(chargeId)) {
        window.clearTimeout(chargeSaveTimersRef.current.get(chargeId));
      }

      const timeoutId = window.setTimeout(async () => {
        chargeSaveTimersRef.current.delete(chargeId);
        try {
          const { charge } = findChargeById(chargeId);
          if (charge) {
            await saveCharge(charge);
          }
        } finally {
          completePendingSave(key);
        }
      }, SAVE_DEBOUNCE_MS);

      chargeSaveTimersRef.current.set(chargeId, timeoutId);
    },
    [completePendingSave, findChargeById, registerPendingSave, saveCharge],
  );

  const updateCase = useCallback(
    (caseId, updater, { queueSave: shouldQueueSave = false } = {}) => {
      let updatedCase = null;
      setMeetingCases((prevCases) =>
        prevCases.map((caseItem) => {
          if (caseItem.id !== caseId) {
            return caseItem;
          }
          updatedCase =
            typeof updater === 'function' ? updater(caseItem) : updater;
          return updatedCase;
        }),
      );
      if (shouldQueueSave && updatedCase) {
        queueCaseSave(caseId);
      }
    },
    [queueCaseSave],
  );

  const updateCharge = useCallback(
    (
      caseId,
      chargeId,
      updater,
      { queueSave: shouldQueueSave = false } = {},
    ) => {
      let updatedCharge = null;
      setMeetingCases((prevCases) =>
        prevCases.map((caseItem) => {
          if (caseItem.id !== caseId) {
            return caseItem;
          }
          const updatedCharges = caseItem.charges.map((charge) => {
            if (charge.id !== chargeId) {
              return charge;
            }
            updatedCharge =
              typeof updater === 'function' ? updater(charge) : updater;
            return updatedCharge;
          });
          return {
            ...caseItem,
            charges: updatedCharges,
          };
        }),
      );
      if (shouldQueueSave && updatedCharge) {
        queueChargeSave(chargeId);
      }
    },
    [queueChargeSave],
  );

  const addPersonAtCase = useCallback(
    async (caseId, person, roleId) => {
      if (!caseId || !person?.id) {
        return;
      }
      const url = `/addPersonAtCase?case_id=${caseId}&personId=${Number(
        person.id,
      )}&role=${roleId}`;
      await post(url, {
        errorMessage: 'Unable to add person to case',
      });
    },
    [post],
  );

  const removePersonAtCase = useCallback(
    async (caseId, person, roleId) => {
      if (!caseId || !person?.id) {
        return;
      }
      const url = `/removePersonAtCase?case_id=${caseId}&personId=${Number(
        person.id,
      )}&role=${roleId}`;
      await post(url, {
        errorMessage: 'Unable to remove person from case',
      });
    },
    [post],
  );

  const handleAddCharge = useCallback(
    async (caseId) => {
      if (!caseId) {
        return;
      }
      const response = await post(`/addCharge?case_id=${caseId}`, {
        errorMessage: 'Unable to add charge',
      });
      const text = await response.text();
      const chargeId = Number(text);
      if (Number.isNaN(chargeId)) {
        handleError('Unable to parse new charge identifier', new Error(text));
        return;
      }
      const newCharge = createEmptyCharge(chargeId);
      setMeetingCases((prevCases) =>
        prevCases.map((caseItem) =>
          caseItem.id === caseId
            ? { ...caseItem, charges: [...caseItem.charges, newCharge] }
            : caseItem,
        ),
      );
    },
    [handleError, post],
  );

  const handleRemoveCharge = useCallback(
    async (caseId, chargeId) => {
      if (!chargeId) {
        return;
      }
      await post(`/removeCharge?id=${chargeId}`, {
        errorMessage: 'Unable to remove charge',
      });
      setMeetingCases((prevCases) =>
        prevCases.map((caseItem) =>
          caseItem.id === caseId
            ? {
                ...caseItem,
                charges: caseItem.charges.filter((charge) => charge.id !== chargeId),
              }
            : caseItem,
        ),
      );
    },
    [post],
  );

  const refreshCaseReferences = useCallback(
    async (caseId) => {
      if (!caseId) {
        return;
      }

      setMeetingCases((prevCases) =>
        prevCases.map((caseItem) =>
          caseItem.id === caseId
            ? { ...caseItem, referencesLoading: true }
            : caseItem,
        ),
      );

      try {
        const response = await get(`/getCaseReferencesJson?case_id=${caseId}`, {
          errorMessage: 'Unable to load case references',
        });
        const data = await response.json();

        const referencedMap = new Map();
        data.forEach((reference) => {
          reference.charges.forEach((refCharge) => {
            if (refCharge.generated_charge_id) {
              referencedMap.set(refCharge.generated_charge_id, {
                id: refCharge.charge_id,
                resolutionPlan: refCharge.resolutionPlan,
                hasDefaultRule: Boolean(refCharge.has_default_rule),
              });
            }
          });
        });

        setMeetingCases((prevCases) =>
          prevCases.map((caseItem) => {
            if (caseItem.id !== caseId) {
              return caseItem;
            }

            const updatedCharges = caseItem.charges.map((charge) => {
              const reference = referencedMap.get(charge.id) || null;
              let followUpChoice = charge.followUpChoice;
              if (reference) {
                if (charge.resolutionPlan === TIME_SERVED_LABEL) {
                  followUpChoice = 'timeServed';
                } else if (charge.resolutionPlan) {
                  followUpChoice = 'newPlan';
                } else {
                  followUpChoice = 'original';
                }
              }
              return {
                ...charge,
                referencedSource: reference,
                followUpChoice,
              };
            });

            return {
              ...caseItem,
              charges: updatedCharges,
              caseReferences: data,
              referencesLoaded: true,
              referencesLoading: false,
            };
          }),
        );
      } catch (error) {
        setMeetingCases((prevCases) =>
          prevCases.map((caseItem) =>
            caseItem.id === caseId
              ? { ...caseItem, referencesLoading: false }
              : caseItem,
          ),
        );
      }
    },
    [get],
  );

  const handleAddReferencedCase = useCallback(
    async (caseId, referencedCaseId) => {
      if (!caseId || !referencedCaseId) {
        return;
      }
      await post(
        `/addReferencedCase?case_id=${caseId}&referenced_case_id=${referencedCaseId}`,
        {
          errorMessage: 'Unable to add referenced case',
        },
      );
      await refreshCaseReferences(caseId);
    },
    [post, refreshCaseReferences],
  );

  const handleRemoveReferencedCase = useCallback(
    async (caseId, referencedCaseId) => {
      if (!caseId || !referencedCaseId) {
        return;
      }
      await post(
        `/removeReferencedCase?case_id=${caseId}&referenced_case_id=${referencedCaseId}`,
        {
          errorMessage: 'Unable to remove referenced case',
        },
      );
      await refreshCaseReferences(caseId);
    },
    [post, refreshCaseReferences],
  );

  const handleToggleReferencedCharge = useCallback(
    async (caseId, referencedChargeId, checked) => {
      if (!caseId || !referencedChargeId) {
        return;
      }
      const baseUrl = checked
        ? '/addChargeReferenceToCase'
        : '/removeChargeReferenceFromCase';
      await post(`${baseUrl}?case_id=${caseId}&charge_id=${referencedChargeId}`, {
        errorMessage: 'Unable to update referenced charge',
      });
      await refreshCaseReferences(caseId);
    },
    [post, refreshCaseReferences],
  );

  const handleGenerateChargeFromReference = useCallback(
    async (caseId, referenceCharge) => {
      const referencedChargeId = referenceCharge?.charge_id;
      if (!caseId || !referencedChargeId) {
        return;
      }

      const response = await post(
        `/generateChargeFromReference?case_id=${caseId}&referenced_charge_id=${referencedChargeId}`,
        {
          errorMessage: 'Unable to generate referenced charge',
        },
      );

      const payload = await response.json();
      const mappedCharge =
        mapChargeFromServer(payload) || createEmptyCharge(payload?.id);

      mappedCharge.referencedSource = {
        id: referenceCharge.charge_id,
        resolutionPlan: referenceCharge.resolutionPlan,
        hasDefaultRule: Boolean(referenceCharge.has_default_rule),
      };
      mappedCharge.followUpChoice = mappedCharge.resolutionPlan
        ? mappedCharge.resolutionPlan === TIME_SERVED_LABEL
          ? 'timeServed'
          : 'newPlan'
        : 'original';

      setMeetingCases((prevCases) =>
        prevCases.map((caseItem) =>
          caseItem.id === caseId
            ? { ...caseItem, charges: [...caseItem.charges, mappedCharge] }
            : caseItem,
        ),
      );

      await refreshCaseReferences(caseId);
    },
    [mapChargeFromServer, post, refreshCaseReferences],
  );

  const handleClearCase = useCallback(
    async (caseId) => {
      if (!caseId) {
        return;
      }

      const currentCases = casesRef.current;
      const caseItem = currentCases.find((item) => item.id === caseId);
      if (!caseItem) {
        return;
      }

      await Promise.all(
        caseItem.charges.map((charge) =>
          post(`/removeCharge?id=${charge.id}`, {
            errorMessage: 'Unable to remove charge',
          }).catch(() => undefined),
        ),
      );

      await Promise.all(
        caseItem.testifiers.map((person) =>
          removePersonAtCase(caseId, person, Number(roleIds.testifier)).catch(
            () => undefined,
          ),
        ),
      );

      if (config.track_writer) {
        await Promise.all(
          caseItem.writers.map((person) =>
            removePersonAtCase(caseId, person, Number(roleIds.writer)).catch(
              () => undefined,
            ),
          ),
        );
      }

      await post(`/clearAllReferencedCases?case_id=${caseId}`, {
        errorMessage: 'Unable to clear referenced cases',
      });

      updateCase(
        caseId,
        (existing) => ({
          ...existing,
          location: '',
          findings: '',
          date: '',
          time: '',
          continued: false,
          charges: [],
          testifiers: [],
          writers: [],
          caseReferences: [],
          referencesLoaded: false,
          referencesLoading: false,
        }),
        { queueSave: true },
      );
    },
    [
      config.track_writer,
      post,
      removePersonAtCase,
      roleIds.testifier,
      roleIds.writer,
      updateCase,
    ],
  );

  const handleAddNewCase = useCallback(async () => {
    if (!meetingId) {
      return;
    }
    const response = await post(`/newCase?meeting_id=${meetingId}`, {
      errorMessage: 'Unable to add new case',
    });
    const payload = await response.json();
    const [caseIdRaw, caseNumber] = payload;
    const caseId = Number(caseIdRaw);
    const newCase = {
      id: caseId,
      caseNumber,
      location: '',
      findings: '',
      date: '',
      time: '',
      continued: false,
      charges: [],
      testifiers: [],
      writers: [],
      caseReferences: [],
      referencesLoaded: false,
      referencesLoading: false,
    };
    setMeetingCases((prevCases) => [...prevCases, newCase]);
  }, [meetingId, post]);

  const handleContinueCase = useCallback(
    async (caseId) => {
      if (!meetingId || !caseId) {
        return;
      }
      const response = await post(
        `/continueCase?meeting_id=${meetingId}&case_id=${caseId}`,
        {
          errorMessage: 'Unable to continue case',
        },
      );
      const payload = await response.json();
      const newCase = mapCaseFromServer(payload);
      if (newCase) {
        setMeetingCases((prevCases) => [...prevCases, newCase]);
        setOpenCases((prev) => prev.filter((item) => item.id !== caseId));
      }
    },
    [mapCaseFromServer, meetingId, post],
  );

  const meetingTitle = config?.str_jc_name || 'RC';
  const printableUrl = window.initialData?.viewPrintableUrl;
  const meetingDate = window.initialData?.meetingDate || '';
  const enableCaseReferences = Boolean(config?.org?.enableCaseReferences);

  const beforeUnloadHandler = useCallback(
    (event) => {
      if (pendingSaveKeysRef.current.size > 0) {
        event.preventDefault();
        event.returnValue = 'You have unsaved changes.';
      }
    },
    [],
  );

  const addPersonToMeeting = useCallback(
    async (person, roleId) => {
      if (!meetingId || !person?.id) {
        return;
      }
      const url = `/addPersonAtMeeting?meeting_id=${meetingId}&personId=${Number(
        person.id,
      )}&role=${roleId}`;
      await post(url, {
        errorMessage: 'Unable to add person to meeting',
      });
    },
    [meetingId, post],
  );

  const removePersonFromMeeting = useCallback(
    async (person, roleId) => {
      if (!meetingId || !person?.id) {
        return;
      }
      const url = `/removePersonAtMeeting?meeting_id=${meetingId}&personId=${Number(
        person.id,
      )}&role=${roleId}`;
      await post(url, {
        errorMessage: 'Unable to remove person from meeting',
      });
    },
    [meetingId, post],
  );

  const loadSidebarContent = useCallback(
    async (url) => {
      if (!url) {
        return;
      }
      setSidebarLoading(true);
      try {
        const response = await get(url, {
          errorMessage: 'Unable to load sidebar content',
        });
        const html = await response.text();
        setSidebarContent(html.replace(/<a\s+nosidebar/gi, '<a'));
      } finally {
        setSidebarLoading(false);
      }
    },
    [get],
  );

  const showPersonHistory = useCallback(
    (personId) => {
      if (!personId) {
        return;
      }
      loadSidebarContent(`/personHistory/${personId}`);
    },
    [loadSidebarContent],
  );

  const showRuleHistory = useCallback(
    (ruleId) => {
      if (!ruleId) {
        return;
      }
      loadSidebarContent(`/ruleHistory/${ruleId}`);
    },
    [loadSidebarContent],
  );

  const showPersonRuleHistory = useCallback(
    (personId, ruleId) => {
      if (!personId || !ruleId) {
        return;
      }
      loadSidebarContent(`/personRuleHistory/${personId}/${ruleId}`);
    },
    [loadSidebarContent],
  );

  const fetchLastResolutionPlan = useCallback(
    async (personId, ruleId) => {
      if (!personId || !ruleId) {
        return '';
      }
      try {
        const response = await get(`/getLastRp/${personId}/${ruleId}`, {
          errorMessage: 'Unable to load previous resolution plan',
        });
        return await response.text();
      } catch (error) {
        return '';
      }
    },
    [get],
  );

  useEffect(() => {
    window.addEventListener('beforeunload', beforeUnloadHandler);
    return () => {
      window.removeEventListener('beforeunload', beforeUnloadHandler);
    };
  }, [beforeUnloadHandler]);

  return (
    <Box
      sx={{
        display: 'flex',
        gap: 3,
        alignItems: 'flex-start',
        flexWrap: 'wrap',
      }}
    >
      <Box
        sx={{
          flex: '1 1 720px',
          minWidth: '60%',
          display: 'flex',
          flexDirection: 'column',
          gap: 3,
        }}
      >
        <Card>
          <CardHeader
            title={
              <Stack
                direction="row"
                justifyContent="space-between"
                alignItems="center"
                spacing={2}
              >
                <Typography variant="h5">
                  {meetingTitle} Minutes — {meetingDate}
                </Typography>
                <Button
                  variant="contained"
                  onClick={() => {
                    if (printableUrl) {
                      window.location.href = printableUrl;
                    }
                  }}
                >
                  View printable minutes
                </Button>
              </Stack>
            }
          />
          <CardContent>
            <Stack spacing={1.5}>
              <Typography variant="body2" color="text.secondary">
                Changes save automatically.
              </Typography>
              <Typography
                variant="body2"
                color={pendingSaveCount > 0 ? 'warning.main' : 'success.main'}
              >
                {pendingSaveCount > 0
                  ? `Saving ${pendingSaveCount} change${
                      pendingSaveCount === 1 ? '' : 's'
                    }…`
                  : 'All changes saved.'}
              </Typography>
            </Stack>
          </CardContent>
        </Card>

        <Card>
          <CardHeader title="Committee & Roles" />
          <CardContent>
            <Stack
              direction="row"
              flexWrap="wrap"
              columnGap={2}
              rowGap={{ xs: 1, md: 2 }}
              sx={{
                '& > *': {
                  flex: {
                    xs: '1 1 100%',
                    md: '1 1 calc(33.333% - 16px)',
                    lg: '1 1 calc(33.333% - 16px)',
                  },
                  minWidth: { xs: '100%', md: 240 },
                },
              }}
            >
              <SelectInput
                autocomplete
                multiple
                label={messages.committeeMembers || 'Committee members'}
                options={peopleOptions.map(p => ({ ...p, value: p.id, label: p.label }))}
                value={committee.map(p => p.id)}
                onChange={(e, newIds) => {
                  const newPeople = (Array.isArray(newIds) ? newIds : []).map(id => peopleOptionMap.get(String(id))).filter(Boolean);
                  // Detect adds / removes
                  const prevIds = new Set(committee.map(p => p.id));
                  const nextIds = new Set(newPeople.map(p => p.id));
                  newPeople.forEach(p => { if (!prevIds.has(p.id)) addPersonToMeeting(p, Number(roleIds.committee)); });
                  committee.forEach(p => { if (!nextIds.has(p.id)) removePersonFromMeeting(p, Number(roleIds.committee)); });
                  setCommittee(newPeople);
                }}
                placeholder="Search people"
                size="medium"
                fullWidth
              />
              <SelectInput
                autocomplete
                label="Chair"
                options={peopleOptions.map(p => ({ ...p, value: p.id, label: p.label }))}
                value={chair[0]?.id || ''}
                onChange={(e, newId) => {
                  const newPerson = peopleOptionMap.get(String(newId));
                  const prev = chair[0];
                  if (newPerson && (!prev || prev.id !== newPerson.id)) {
                    addPersonToMeeting(newPerson, Number(roleIds.chair));
                  }
                  if (prev && (!newPerson || prev.id !== newPerson.id)) {
                    removePersonFromMeeting(prev, Number(roleIds.chair));
                  }
                  setChair(newPerson ? [newPerson] : []);
                }}
                placeholder="Search people"
                size="medium"
                fullWidth
              />
              <SelectInput
                autocomplete
                label="Notetaker"
                options={peopleOptions.map(p => ({ ...p, value: p.id, label: p.label }))}
                value={noteTaker[0]?.id || ''}
                onChange={(e, newId) => {
                  const newPerson = peopleOptionMap.get(String(newId));
                  const prev = noteTaker[0];
                  if (newPerson && (!prev || prev.id !== newPerson.id)) {
                    addPersonToMeeting(newPerson, Number(roleIds.noteTaker));
                  }
                  if (prev && (!newPerson || prev.id !== newPerson.id)) {
                    removePersonFromMeeting(prev, Number(roleIds.noteTaker));
                  }
                  setNoteTaker(newPerson ? [newPerson] : []);
                }}
                placeholder="Search people"
                size="medium"
                fullWidth
              />
              <SelectInput
                autocomplete
                multiple
                label="Subs"
                options={peopleOptions.map(p => ({ ...p, value: p.id, label: p.label }))}
                value={subs.map(p => p.id)}
                onChange={(e, newIds) => {
                  const newPeople = (Array.isArray(newIds) ? newIds : []).map(id => peopleOptionMap.get(String(id))).filter(Boolean);
                  const prevIds = new Set(subs.map(p => p.id));
                  const nextIds = new Set(newPeople.map(p => p.id));
                  newPeople.forEach(p => { if (!prevIds.has(p.id)) addPersonToMeeting(p, Number(roleIds.sub)); });
                  subs.forEach(p => { if (!nextIds.has(p.id)) removePersonFromMeeting(p, Number(roleIds.sub)); });
                  setSubs(newPeople);
                }}
                placeholder="Search people"
                size="medium"
                fullWidth
              />
              <SelectInput
                autocomplete
                multiple
                label="Runners"
                options={peopleOptions.map(p => ({ ...p, value: p.id, label: p.label }))}
                value={runners.map(p => p.id)}
                onChange={(e, newIds) => {
                  const newPeople = (Array.isArray(newIds) ? newIds : []).map(id => peopleOptionMap.get(String(id))).filter(Boolean);
                  const prevIds = new Set(runners.map(p => p.id));
                  const nextIds = new Set(newPeople.map(p => p.id));
                  newPeople.forEach(p => { if (!prevIds.has(p.id)) addPersonToMeeting(p, Number(roleIds.runner)); });
                  runners.forEach(p => { if (!nextIds.has(p.id)) removePersonFromMeeting(p, Number(roleIds.runner)); });
                  setRunners(newPeople);
                }}
                placeholder="Search people"
                size="medium"
                fullWidth
              />
            </Stack>
          </CardContent>
        </Card>

        {meetingCases.map((caseItem) => (
          <CaseCard
            key={caseItem.id}
            caseItem={caseItem}
            config={config}
            messages={messages}
            peopleOptions={peopleOptions}
            peopleOptionMap={peopleOptionMap}
            ruleOptions={ruleOptions}
            caseOptions={caseOptions}
            roleIds={roleIds}
            enableCaseReferences={enableCaseReferences}
            onUpdateCase={updateCase}
            onUpdateCharge={updateCharge}
            onAddCharge={handleAddCharge}
            onRemoveCharge={handleRemoveCharge}
            onAddPersonAtCase={addPersonAtCase}
            onRemovePersonAtCase={removePersonAtCase}
            onAddReferencedCase={handleAddReferencedCase}
            onRemoveReferencedCase={handleRemoveReferencedCase}
            onToggleReferencedCharge={handleToggleReferencedCharge}
            onGenerateChargeFromReference={handleGenerateChargeFromReference}
            onRequestClearCase={setCaseToConfirmClear}
            onRefreshReferences={refreshCaseReferences}
            onShowPersonRuleHistory={showPersonRuleHistory}
            fetchLastResolutionPlan={fetchLastResolutionPlan}
          />
        ))}

        <Stack direction="row" spacing={2} justifyContent="flex-start">
          <Button variant="contained" onClick={handleAddNewCase}>
            {messages.addNewCase || 'Add new case'}
          </Button>
        </Stack>

        {visibleOpenCases.length > 0 && (
          <Card>
            <CardHeader
              title={messages.casesToBeContinued || 'Cases to be continued'}
            />
            <CardContent>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                {messages.chooseCaseToContinue || 'Choose a case to continue'}
              </Typography>
              <Stack spacing={1.5}>
                {visibleOpenCases.map((openCase) => (
                  <Paper
                    key={openCase.id}
                    sx={{
                      p: 1.5,
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                    }}
                  >
                    <Box>
                      <Typography variant="subtitle2">
                        {openCase.caseNumber}
                      </Typography>
                      <Typography variant="body2" color="text.secondary">
                        {[openCase.location, openCase.findings]
                          .filter(Boolean)
                          .map((text) =>
                            text.length > 80 ? `${text.slice(0, 80)}…` : text,
                          )
                          .join(' • ')}
                      </Typography>
                    </Box>
                    <Button
                      size="small"
                      variant="outlined"
                      onClick={() => handleContinueCase(openCase.id)}
                    >
                      Continue
                    </Button>
                  </Paper>
                ))}
              </Stack>
            </CardContent>
          </Card>
        )}
      </Box>

      <Box
        sx={{
          flex: '0 0 320px',
          display: 'flex',
          flexDirection: 'column',
          gap: 2,
        }}
      >
        <Paper sx={{ p: 2, minHeight: 320 }}>
          {sidebarLoading ? (
            <Typography variant="body2">Loading…</Typography>
          ) : (
            <div
              onClick={(event) => {
                const infoLink = event.target.closest('.more-info');
                if (infoLink) {
                  event.preventDefault();
                  const personId = infoLink.getAttribute('data-person-id');
                  const ruleId = infoLink.getAttribute('data-rule-id');
                  if (personId && ruleId) {
                    showPersonRuleHistory(personId, ruleId);
                  }
                }
              }}
              dangerouslySetInnerHTML={{ __html: sidebarContent }}
            />
          )}
        </Paper>
      </Box>

      <DeleteDialog
        open={Boolean(caseToConfirmClear)}
        title={messages.eraseCase || 'Erase case'}
        message={
          messages.eraseCaseConfirmation ||
          'Are you sure you want to erase this case?'
        }
        handleClose={() => setCaseToConfirmClear(null)}
        handleConfirm={async () => {
          if (!caseToConfirmClear) {
            setCaseToConfirmClear(null);
            return;
          }
          try {
            await handleClearCase(caseToConfirmClear);
          } finally {
            setCaseToConfirmClear(null);
          }
        }}
      />
    </Box>
  );
};

export default EditMinutesPage;
