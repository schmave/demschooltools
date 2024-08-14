const Handlebars = require('handlebars');
const autocomplete = require('./autocomplete');

Handlebars.registerHelper('ifEquals', function(arg1, arg2, options) {
    return (arg1 == arg2) ? options.fn(this) : options.inverse(this);
});

export function init(roles, people, terms) {
    sortAll(roles);
    registerTabEvents(roles, terms);
    switchTab(roles, terms, people, 'Individual');
}

function registerTabEvents(roles, terms, people) {
    const tabs = document.getElementsByClassName('roles-tab');
    for (const tab of tabs) {
        const roleType = tab.getAttribute('data-type');
        tab.addEventListener('click', () => {
            switchTab(roles, terms, people, roleType);
        });
    }
}

function switchTab(roles, terms, people, roleType) {
    const tabs = document.getElementsByClassName('roles-tab');
    for (const tab of tabs) {
        if (tab.getAttribute('data-type') === roleType) {
            tab.classList.add('active');
        } else {
            tab.classList.remove('active');
        }
    }
    const filteredRoles = roles.filter(r => r.type === roleType);
    renderIndex(filteredRoles, terms, people, roleType);
}

function renderIndex(roles, terms, people, roleType) {
    const container = document.getElementById('roles-index-container');
    if (!roles || !roles.length) {
        container.innerHTML = '<div style="padding:15px;">No roles have been created yet with this type.</div>';
        return;
    }
    const template = Handlebars.compile($('#roles-index-template').html());
    container.innerHTML = template({
        chairHeader: getChairHeader(roleType, terms),
        hasChairs: roles.some(r => getMembers(r, 'Chair').length > 0),
        hasBackups: roles.some(r => getMembers(r, 'Backup').length > 0),
        hasMembers: roles.some(r => getMembers(r, 'Member').length > 0),
        hasEligibilities: roles.some(r => r.eligibility !== 'Anyone'),
        hasNotes: roles.some(r => !!r.notes),
        hasDescriptions: roles.some(r => !!r.description),
        roles: roles.map(role => {
            return {
                id: role.id,
                name: role.name,
                chairs: formatMemberList(role, 'Chair'),
                backups: formatMemberList(role, 'Backup'),
                members: formatMemberList(role, 'Member'),
                eligibility: formatEligibility(role.eligibility),
                notes: role.notes,
                description: role.description
            };
        })
    });
    const editButtons = document.getElementsByClassName('roles-edit-button');
    for (const editButton of editButtons) {
        editButton.addEventListener('click', () => {
            const roleId = editButton.getAttribute('data-id');
            const role = roles.filter(r => r.id == roleId)[0];
            renderEditor(role, people);
        });
    }
    sorttable.makeSortable(container.querySelector('table'));
}

function renderEditor(role, people) {
    document.querySelector('.roles-editor').style.display = 'block';
    const table = document.querySelector('.roles-edit-table');
    const generalTemplate = Handlebars.compile($('#roles-editor-general-template').html());
    const generalHtml = generalTemplate({
        name: role.name,
        type: role.type,
        eligibility: role.eligibility,
        notes: role.notes,
        description: role.description
    });
    table.innerHTML = generalHtml;
    table.innerHTML += getEditorSpecialHtml();

    const getPeopleResults = renderEditorPeople(role, people);
    registerEditorEvents(role, getPeopleResults);
}

function getEditorSpecialHtml() {
    if (role.type === 'Individual') {
        const template = Handlebars.compile($('#roles-editor-individual-template').html());
        return template({
            chairs: getMembers(role, 'Chair'),
            backups: getMembers(role, 'Backup')
        });
    } else {
        const template = Handlebars.compile($('#roles-editor-group-template').html());
        return template({
            chairs: getMembers(role, 'Chair'),
            members: getMembers(role, 'Member')
        });
    }
}

function renderEditorPeople(role, people) {
    const getResults = {};
    const chairsContainer = $('.js-roles-editor-chairs');
    const backupsContainer = $('.js-roles-editor-backups');
    const membersContainer = $('.js-roles-editor-members');
    const opts = {
        multi: true,
        allowPlainText: true,
        autoAdvance: true
    };
    if (chairsContainer) {
        getResults.chairs = autocomplete.registerAutocomplete(chairsContainer, people, getMembers(role, 'Chair'), opts);
    }
    if (backupsContainer) {
        getResults.backups = autocomplete.registerAutocomplete(backupsContainer, people, getMembers(role, 'Backup'), opts);
    }
    if (membersContainer) {
        getResults.members = autocomplete.registerAutocomplete(membersContainer, people, getMembers(role, 'Member'), opts);
    }
    return getResults;
}

function registerEditorEvents(role, getPeopleResults) {
    const buttonsContainer = document.getElementById('roles-editor-buttons');
    const template = Handlebars.compile(document.getElementById('roles-editor-buttons-template').innerHTML);
    buttonsContainer.innerHTML = template({});
    const submitButton = buttonsContainer.getElementById('roles-editor-submit');
    const cancelButton = buttonsContainer.getElementById('roles-editor-cancel');
    submitButton.addEventListener('click', () => {
        saveRole(role, getPeopleResults);
        updateIndex(role);
        closeEditor();
    });
    cancelButton.addEventListener('click', () => {
        closeEditor();
    });
}

function saveRole(role, getPeopleResults) {
    const role = JSON.stringify({
        eligibility: role.eligibility,
        name: role.name,
        notes: role.notes,
        description: role.description,
        is_active: role.is_active
    });
    const chairs = autocompleteResultToJson(getPeopleResults.chairs);
    const backups = autocompleteResultToJson(getPeopleResults.backups);
    const members = autocompleteResultToJson(getPeopleResults.members);
    const queryString = `?role=${role}&chairs=${chairs}&backups=${backups}&members=${members}`;
    const url = `/roles/updateRole/${role.id}${queryString}`;
    fetch(url, { method: 'POST' });
}

function autocompleteResultToJson(result) {
    return JSON.stringify(result().map(r => {
        return {
            label: r.label,
            id: r.id
        };
    }));
}

function updateIndex(role) {

}

function closeEditor() {
    document.querySelector('.roles-editor').style.display = 'none';
}

function getChairHeader(roleType, terms) {
    if (roleType === 'Individual') {
        return terms.individual;
    }
    return 'Chair';
}

function getMembers(role, memberType) {
    if (!role.records || !role.records.length) {
        return [];
    }
    const lastRecord = role.records[role.records.length - 1];
    if (!lastRecord.members) {
        return [];
    }
    return lastRecord.members.filter(m => m.type === memberType);
}

function formatMemberList(role, memberType) {
    const members = getMembers(role, memberType);
    return members.map(m => getMemberName(m)).filter(m => !!m).join(', ');
}

function formatEligibility(eligibility) {
    if (eligibility === 'StaffOnly') {
        return 'Staff Only';
    }
    if (eligibility === 'StudentOnly') {
        return 'Student Only';
    }
    return '';
}

function getMemberName(member) {
    if (member.person) {
        return member.person.displayName || '';
    }
    return member.personName || '';
}

function sortAll(roles) {
    sortRoles(roles);
    for (const role of roles) {
        if (role.records) {
            sortRecords(role.records);
            for (const record of role.records) {
                if (record.members) {
                    sortMembers(record.members);
                }
            }
        }
    }
}

function sortRoles(roles) {
    sortAlphabetically(roles, r => r.name);
}

function sortRecords(records) {
    // sorts chronologically
    records.sort((a, b) => sortFunction(a.dateCreated, b.dateCreated));
    function sortFunction(a, b) {
        // TODO IMPLEMENT
        return 0;
    }
}

function sortMembers(members) {
    sortAlphabetically(members, m => getMemberName(m));
}

function sortAlphabetically(arr, nameFn) {
    arr.sort((a, b) => {
        const _a = nameFn(a).toLowerCase();
        const _b = nameFn(b).toLowerCase();
        if (_a > _b) return 1;
        if (_a < _b) return -1;
        return 0;
    });
}
