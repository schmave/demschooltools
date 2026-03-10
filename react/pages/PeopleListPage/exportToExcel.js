import * as XLSX from 'xlsx';

/**
 * Export the currently visible/filtered MRT data to an Excel file.
 *
 * @param {object} table - The MRT table instance
 * @param {object} [options]
 * @param {boolean} [options.selectedOnly] - If true, export only selected rows
 */
export function exportToExcel(table, { selectedOnly = false } = {}) {
  const visibleColumns = table.getVisibleLeafColumns().filter(
    // MRT adds internal columns like mrt-row-select, mrt-row-expand, etc.
    (col) => !col.id.startsWith('mrt-row') && col.id !== 'actions',
  );

  const rows = selectedOnly
    ? table.getSelectedRowModel().rows
    : table.getFilteredRowModel().rows;

  // Build header row
  const headers = visibleColumns.map((col) => col.columnDef.header);

  // Build data rows
  const data = rows.map((row) =>
    visibleColumns.map((col) => {
      const value = row.getValue(col.id);
      // Keep numbers and booleans as-is for proper Excel types
      if (value === null || value === undefined) return '';
      return value;
    }),
  );

  // Create worksheet
  const ws = XLSX.utils.aoa_to_sheet([headers, ...data]);

  // Auto-width columns based on content
  ws['!cols'] = visibleColumns.map((col, i) => {
    const headerLen = String(headers[i] || '').length;
    let maxLen = headerLen;
    for (const row of data) {
      const cellLen = String(row[i] ?? '').length;
      if (cellLen > maxLen) maxLen = cellLen;
    }
    return { wch: Math.min(Math.max(maxLen + 2, 10), 60) };
  });

  // Create workbook and trigger download
  const wb = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(wb, ws, 'People');

  const today = new Date().toISOString().slice(0, 10);
  XLSX.writeFile(wb, `People_${today}.xlsx`);
}
