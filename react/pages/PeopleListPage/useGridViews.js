import { useState, useCallback } from 'react';
import { createGridView, updateGridView, deleteGridView } from './api';
import { safeParse } from '../../utils';

const DEFAULT_VIEW_STATE = {
  columnVisibility: {
    address: false,
    city: false,
    state: false,
    zip: false,
    neighborhood: false,
    previous_school: false,
    school_district: false,
    notes: false,
  },
  columnOrder: [],
  columnSizing: {},
  sorting: [],
  columnFilters: [],
  columnPinning: { left: ['mrt-row-select', 'actions'], right: [] },
  density: 'compact',
  globalFilter: '',
  wrapText: false,
};

const getInitialViews = () => {
  if (typeof window === 'undefined') return [];
  const raw = window.initialData?.savedViews;
  if (Array.isArray(raw)) return raw;
  return safeParse(raw, []);
};

/**
 * Hook for managing saved grid view configurations.
 * Persists to the database via API calls.
 */
export const useGridViews = () => {
  const [savedViews, setSavedViews] = useState(getInitialViews);
  const [activeViewId, setActiveViewId] = useState(() => {
    // Check URL for ?view={id} on initial load
    try {
      const params = new URLSearchParams(window.location.search);
      const viewId = params.get('view');
      if (viewId) return Number(viewId);
    } catch {
      // ignore
    }
    return null;
  });

  const viewList = savedViews.map((v) => ({
    id: v.id,
    name: v.name,
    is_private: v.is_private,
    created_by: v.created_by,
    created_by_name: v.created_by_name,
  }));

  const saveView = useCallback(
    async (name, gridState, isPrivate = false) => {
      // Check if updating an existing view with the same name owned by current user
      const currentUserId = window.initialData?.currentUserId;
      const existing = savedViews.find(
        (v) => v.name === name && v.created_by === currentUserId,
      );

      if (existing) {
        // Update existing view
        const updated = await updateGridView(existing.id, {
          name,
          state: gridState,
          is_private: isPrivate,
        });
        setSavedViews((prev) =>
          prev.map((v) => (v.id === existing.id ? updated : v)),
        );
        setActiveViewId(updated.id);
        return updated;
      } else {
        // Create new view
        const created = await createGridView({
          name,
          entity_type: 'person',
          state: gridState,
          is_private: isPrivate,
        });
        setSavedViews((prev) => [...prev, created]);
        setActiveViewId(created.id);
        return created;
      }
    },
    [savedViews],
  );

  const loadView = useCallback(
    (id) => {
      if (id === '__default__') {
        setActiveViewId(null);
        return { ...DEFAULT_VIEW_STATE };
      }
      const view = savedViews.find((v) => v.id === id);
      if (view) {
        setActiveViewId(view.id);
        return view.state;
      }
      return null;
    },
    [savedViews],
  );

  const removeView = useCallback(
    async (id) => {
      await deleteGridView(id);
      setSavedViews((prev) => prev.filter((v) => v.id !== id));
      if (activeViewId === id) {
        setActiveViewId(null);
      }
    },
    [activeViewId],
  );

  // If URL has ?view={id}, resolve initial state from that view
  const getInitialViewState = () => {
    if (activeViewId) {
      const view = savedViews.find((v) => v.id === activeViewId);
      if (view?.state) {
        return { ...DEFAULT_VIEW_STATE, ...view.state };
      }
    }
    return DEFAULT_VIEW_STATE;
  };

  return {
    viewList,
    activeViewId,
    saveView,
    loadView,
    deleteView: removeView,
    defaultViewState: getInitialViewState(),
  };
};
