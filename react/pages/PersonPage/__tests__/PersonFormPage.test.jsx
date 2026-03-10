import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { SnackbarContext } from '../../../contexts';
import PersonFormPage from '../PersonFormPage';

const mockJsonResponse = (data, { ok = true, status = 200 } = {}) => ({
  ok,
  status,
  headers: {
    get: (key) => (key.toLowerCase() === 'content-type' ? 'application/json' : null),
  },
  json: () => Promise.resolve(data),
  text: () => Promise.resolve(typeof data === 'string' ? data : JSON.stringify(data)),
});

const renderWithContext = (ui) => {
  const mockSetSnackbar = vi.fn();
  return {
    ...render(
      <SnackbarContext.Provider value={{ snackbar: {}, setSnackbar: mockSetSnackbar }}>
        {ui}
      </SnackbarContext.Provider>,
    ),
    mockSetSnackbar,
  };
};

describe('PersonFormPage', () => {
  beforeEach(() => {
    window.initialData = {
      mode: 'create',
      personApiUrl: '/api/people/',
      customFields: JSON.stringify([]),
      tagOptions: JSON.stringify([{ id: 1, label: 'Student' }]),
      peopleOptions: JSON.stringify([{ id: 10, label: 'Jane Doe' }]),
      showElectronicSignin: false,
    };
    vi.spyOn(global, 'fetch');
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('renders the create form with core fields', () => {
    renderWithContext(<PersonFormPage />);

    expect(screen.getByText('Add a New Person')).toBeInTheDocument();
    expect(screen.getByLabelText('First Name *')).toBeInTheDocument();
    expect(screen.getByLabelText('Last Name *')).toBeInTheDocument();
    expect(screen.getByLabelText('Email')).toBeInTheDocument();
    expect(screen.getByText('Basic Info')).toBeInTheDocument();
    expect(screen.getByText('Contact')).toBeInTheDocument();
    expect(screen.getByText('Student Info')).toBeInTheDocument();
  });

  it('does not show PIN field when showElectronicSignin is false', () => {
    renderWithContext(<PersonFormPage />);

    expect(screen.queryByLabelText('Attendance PIN')).not.toBeInTheDocument();
  });

  it('shows PIN field when showElectronicSignin is true', () => {
    window.initialData.showElectronicSignin = true;
    renderWithContext(<PersonFormPage />);

    expect(screen.getByLabelText('Attendance PIN')).toBeInTheDocument();
  });

  it('validates required fields before submission', async () => {
    const user = userEvent.setup();
    renderWithContext(<PersonFormPage />);

    await user.click(screen.getByRole('button', { name: /Create Person/i }));

    expect(await screen.findByText('First Name is required.')).toBeInTheDocument();
    expect(screen.getByText('Last Name is required.')).toBeInTheDocument();
    // Should not have called the API
    expect(global.fetch).not.toHaveBeenCalled();
  });

  it('submits the form with correct payload shape', async () => {
    global.fetch.mockResolvedValueOnce(
      mockJsonResponse({
        id: 42,
        first_name: 'Alice',
        last_name: 'Smith',
      }),
    );

    const user = userEvent.setup();
    // Prevent navigation
    delete window.location;
    window.location = { href: '' };

    renderWithContext(<PersonFormPage />);

    await user.type(screen.getByLabelText('First Name *'), 'Alice');
    await user.type(screen.getByLabelText('Last Name *'), 'Smith');
    await user.click(screen.getByRole('button', { name: /Create Person/i }));

    await waitFor(() => {
      expect(global.fetch).toHaveBeenCalled();
    });

    const postCall = global.fetch.mock.calls.find(
      ([, options]) => options?.method === 'POST',
    );
    expect(postCall).toBeTruthy();
    const body = JSON.parse(postCall[1].body);
    expect(body.first_name).toBe('Alice');
    expect(body.last_name).toBe('Smith');
    expect(body).toHaveProperty('phone_numbers');
    expect(body).toHaveProperty('tag_ids');
    expect(body).toHaveProperty('custom_field_values');
  });

  it('renders custom fields alongside core fields', () => {
    window.initialData.customFields = JSON.stringify([
      {
        id: 99,
        entity_type: 'person',
        field_type: 'text',
        label: 'Nickname',
        help_text: 'Preferred nickname',
        enabled: true,
        required: false,
        disabled: false,
        display_order: 50000,
        type_props: {},
        type_validation: {},
        visible_to_role_ids: [],
        editable_by_role_ids: [],
      },
    ]);

    renderWithContext(<PersonFormPage />);

    expect(screen.getByLabelText('Nickname')).toBeInTheDocument();
    expect(screen.getByText('Additional Fields')).toBeInTheDocument();
  });
});
