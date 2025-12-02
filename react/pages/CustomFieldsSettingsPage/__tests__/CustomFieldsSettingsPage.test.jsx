import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import CustomFieldsSettingsPage from '../CustomFieldsSettingsPage';

const mockJsonResponse = (data, { ok = true, status = 200 } = {}) => ({
  ok,
  status,
  headers: {
    get: (key) => (key.toLowerCase() === 'content-type' ? 'application/json' : null),
  },
  json: () => Promise.resolve(data),
  text: () =>
    Promise.resolve(typeof data === 'string' ? data : JSON.stringify(data)),
});

const setupInitialData = () => {
  window.initialData = {
    customFieldsApiBase: '/api/custom-fields/',
    roleKeysApiUrl: '/api/role-keys/',
    tagOptions: JSON.stringify([{ id: 1, label: 'Aftercare' }]),
  };
};

describe('CustomFieldsSettingsPage', () => {
  beforeEach(() => {
    setupInitialData();
    vi.spyOn(global, 'fetch');
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('renders default entity type and switches to tags', async () => {
    global.fetch
      .mockResolvedValueOnce(mockJsonResponse({ roles: [] }))
      .mockResolvedValueOnce(mockJsonResponse([{ id: 1, label: 'Nickname', entity_type: 'person', field_type: 'text', required: false, enabled: true, disabled: false, display_order: 5 }]))
      .mockResolvedValueOnce(mockJsonResponse([]));

    const user = userEvent.setup();

    render(<CustomFieldsSettingsPage />);

    await screen.findByText('Nickname');
    expect(screen.getByText('Custom Fields')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /Tags/i }));

    await waitFor(() => {
      const calls = global.fetch.mock.calls.filter(([url]) =>
        url.includes('?entity_type=tag'),
      );
      expect(calls.length).toBeGreaterThan(0);
    });
    expect(screen.getByText('Tag Title')).toBeInTheDocument();
  });

  it('validates label before creating a field', async () => {
    global.fetch
      .mockResolvedValueOnce(mockJsonResponse({ roles: [] }))
      .mockResolvedValueOnce(mockJsonResponse([]));

    const user = userEvent.setup();

    render(<CustomFieldsSettingsPage />);

    await user.click(screen.getByRole('button', { name: /Add Field/i }));
    await user.click(screen.getByRole('button', { name: /Save/i }));

    expect(await screen.findByText('Label is required.')).toBeInTheDocument();
    expect(global.fetch).toHaveBeenCalledTimes(2);
  });

  it('creates a new custom field', async () => {
    global.fetch
      .mockResolvedValueOnce(mockJsonResponse({ roles: [] }))
      .mockResolvedValueOnce(mockJsonResponse([]))
      .mockResolvedValueOnce(
        mockJsonResponse({
          id: 2,
          label: 'Emergency Contact',
          entity_type: 'person',
          field_type: 'text',
          required: false,
          enabled: true,
          disabled: false,
          display_order: 10,
          type_props: {},
          type_validation: {},
        }),
      );

    const user = userEvent.setup();

    render(<CustomFieldsSettingsPage />);

    await user.click(screen.getByRole('button', { name: /Add Field/i }));
    await user.type(screen.getByLabelText('Label'), 'Emergency Contact');
    await user.click(screen.getByRole('button', { name: /Save/i }));

    await screen.findByText('Emergency Contact');

    const postCall = global.fetch.mock.calls.find(([, options]) => options?.method === 'POST');
    expect(postCall).toBeTruthy();
    expect(JSON.parse(postCall[1].body).entity_type).toBe('person');
  });

  it('reorders custom fields and issues patch requests', async () => {
    const fields = [
      {
        id: 1,
        label: 'Nickname',
        entity_type: 'person',
        field_type: 'text',
        required: false,
        enabled: true,
        disabled: false,
        display_order: 1,
        type_props: {},
        type_validation: {},
      },
      {
        id: 2,
        label: 'Locker Number',
        entity_type: 'person',
        field_type: 'integer',
        required: false,
        enabled: true,
        disabled: false,
        display_order: 2,
        type_props: {},
        type_validation: {},
      },
    ];

    global.fetch
      .mockResolvedValueOnce(mockJsonResponse({ roles: [] }))
      .mockResolvedValueOnce(mockJsonResponse(fields))
      .mockResolvedValueOnce(mockJsonResponse({ ...fields[0], display_order: 3 }))
      .mockResolvedValueOnce(mockJsonResponse({ ...fields[1], display_order: 1 }));

    const user = userEvent.setup();

    render(<CustomFieldsSettingsPage />);

    await screen.findByText('Nickname');

    const moveDownButtons = screen.getAllByRole('button', { name: /Move down/i });
    await user.click(moveDownButtons[0]);

    await waitFor(() => {
      const patchCalls = global.fetch.mock.calls.filter(
        ([, options]) => options?.method === 'PATCH',
      );
      expect(patchCalls.length).toBeGreaterThan(0);
    });
  });
});
