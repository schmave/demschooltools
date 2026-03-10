import React, { useState } from 'react';
import {
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  FormControlLabel,
  IconButton,
  ListItemIcon,
  ListItemText,
  Menu,
  MenuItem,
  Switch,
  TextField,
  Tooltip,
} from '@mui/material';
import SaveIcon from '@mui/icons-material/Save';
import ViewListIcon from '@mui/icons-material/ViewList';
import DeleteIcon from '@mui/icons-material/Delete';
import RestoreIcon from '@mui/icons-material/Restore';
import LinkIcon from '@mui/icons-material/Link';
import CheckIcon from '@mui/icons-material/Check';
import LockIcon from '@mui/icons-material/Lock';

const ViewsToolbar = ({
  viewList,
  activeViewId,
  currentUserId,
  onSave,
  onLoad,
  onDelete,
  onShareFeedback,
}) => {
  const [menuAnchor, setMenuAnchor] = useState(null);
  const [saveDialogOpen, setSaveDialogOpen] = useState(false);
  const [viewNameInput, setViewNameInput] = useState('');
  const [isPrivate, setIsPrivate] = useState(false);
  const [copied, setCopied] = useState(false);
  const [deleteConfirm, setDeleteConfirm] = useState(null);
  const [overwriteConfirm, setOverwriteConfirm] = useState(null);

  const activeView = viewList.find((v) => v.id === activeViewId);

  const handleOpenMenu = (e) => setMenuAnchor(e.currentTarget);
  const handleCloseMenu = () => setMenuAnchor(null);

  const handleOpenSaveDialog = () => {
    setViewNameInput(activeView?.name || '');
    setIsPrivate(activeView?.is_private || false);
    setSaveDialogOpen(true);
  };

  const handleSave = () => {
    const name = viewNameInput.trim();
    if (!name) return;
    // Check if a view with this name already exists (owned by current user)
    const existing = viewList.find(
      (v) => v.name === name && v.created_by === currentUserId,
    );
    if (existing) {
      setSaveDialogOpen(false);
      setOverwriteConfirm(name);
    } else {
      onSave(name, isPrivate);
      setSaveDialogOpen(false);
      setViewNameInput('');
      setIsPrivate(false);
    }
  };

  const handleConfirmOverwrite = () => {
    onSave(overwriteConfirm, isPrivate);
    setOverwriteConfirm(null);
    setViewNameInput('');
    setIsPrivate(false);
  };

  const handleLoadView = (id) => {
    onLoad(id);
    handleCloseMenu();
  };

  const handleDeleteView = (e, id) => {
    e.stopPropagation();
    const view = viewList.find((v) => v.id === id);
    setDeleteConfirm({ id, name: view?.name || '' });
  };

  const handleConfirmDelete = () => {
    onDelete(deleteConfirm.id);
    setDeleteConfirm(null);
  };

  const handleShare = async () => {
    if (!activeViewId || activeView?.is_private) return;
    const url = `${window.location.origin}/people/list?view=${activeViewId}`;
    try {
      await navigator.clipboard.writeText(url);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
      if (onShareFeedback) onShareFeedback();
    } catch {
      // Fallback: ignore if clipboard API is unavailable
    }
  };

  const shareDisabled = !activeViewId || activeView?.is_private;

  return (
    <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
      <Tooltip title="Save current view">
        <IconButton onClick={handleOpenSaveDialog} size="small">
          <SaveIcon />
        </IconButton>
      </Tooltip>

      <Tooltip title="Load saved view">
        <IconButton onClick={handleOpenMenu} size="small">
          <ViewListIcon />
        </IconButton>
      </Tooltip>

      <Tooltip
        title={
          shareDisabled
            ? 'Save a shared view first to get a link'
            : copied
              ? 'Link copied!'
              : 'Copy shareable link'
        }
      >
        <span>
          <IconButton
            onClick={handleShare}
            size="small"
            color={copied ? 'success' : 'default'}
            disabled={shareDisabled}
          >
            {copied ? <CheckIcon /> : <LinkIcon />}
          </IconButton>
        </span>
      </Tooltip>

      <Menu anchorEl={menuAnchor} open={Boolean(menuAnchor)} onClose={handleCloseMenu}>
        <MenuItem onClick={() => handleLoadView('__default__')}>
          <ListItemIcon>
            <RestoreIcon fontSize="small" />
          </ListItemIcon>
          <ListItemText>Default View</ListItemText>
        </MenuItem>

        {viewList.length > 0 && <MenuItem divider disabled sx={{ p: 0, minHeight: 0 }} />}

        {viewList.map((view) => (
          <MenuItem key={view.id} onClick={() => handleLoadView(view.id)}>
            {view.is_private && (
              <ListItemIcon sx={{ minWidth: 28 }}>
                <LockIcon fontSize="small" color="action" />
              </ListItemIcon>
            )}
            <ListItemText
              primary={view.name}
              secondary={
                view.created_by !== currentUserId
                  ? `by ${view.created_by_name}`
                  : undefined
              }
              primaryTypographyProps={{
                fontWeight: view.id === activeViewId ? 600 : 400,
              }}
            />
            {view.created_by === currentUserId && (
              <IconButton
                size="small"
                onClick={(e) => handleDeleteView(e, view.id)}
                sx={{ ml: 1 }}
              >
                <DeleteIcon fontSize="small" />
              </IconButton>
            )}
          </MenuItem>
        ))}

        {viewList.length === 0 && (
          <MenuItem disabled>
            <ListItemText secondary="No saved views" />
          </MenuItem>
        )}
      </Menu>

      <Dialog
        open={saveDialogOpen}
        onClose={() => setSaveDialogOpen(false)}
        maxWidth="xs"
        fullWidth
      >
        <DialogTitle>Save View</DialogTitle>
        <DialogContent>
          <TextField
            autoFocus
            label="View name"
            fullWidth
            value={viewNameInput}
            onChange={(e) => setViewNameInput(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter') handleSave();
            }}
            sx={{ mt: 1 }}
          />
          <FormControlLabel
            control={
              <Switch
                checked={isPrivate}
                onChange={(e) => setIsPrivate(e.target.checked)}
              />
            }
            label="Private (only visible to me)"
            sx={{ mt: 1 }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setSaveDialogOpen(false)}>Cancel</Button>
          <Button onClick={handleSave} variant="contained" disabled={!viewNameInput.trim()}>
            Save
          </Button>
        </DialogActions>
      </Dialog>

      {/* Delete confirmation dialog */}
      <Dialog open={Boolean(deleteConfirm)} onClose={() => setDeleteConfirm(null)}>
        <DialogTitle>Delete View</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Delete view &apos;{deleteConfirm?.name}&apos;? This cannot be undone.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteConfirm(null)}>Cancel</Button>
          <Button onClick={handleConfirmDelete} color="error" variant="contained">
            Delete
          </Button>
        </DialogActions>
      </Dialog>

      {/* Overwrite confirmation dialog */}
      <Dialog open={Boolean(overwriteConfirm)} onClose={() => setOverwriteConfirm(null)}>
        <DialogTitle>Overwrite View</DialogTitle>
        <DialogContent>
          <DialogContentText>
            A view named &apos;{overwriteConfirm}&apos; already exists. Overwrite it?
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => { setOverwriteConfirm(null); setSaveDialogOpen(true); }}>Cancel</Button>
          <Button onClick={handleConfirmOverwrite} variant="contained">
            Overwrite
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default ViewsToolbar;
