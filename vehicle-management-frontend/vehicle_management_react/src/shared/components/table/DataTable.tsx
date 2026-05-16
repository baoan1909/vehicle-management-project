import { useEffect, useMemo, useState } from "react";
import type { TableColumn } from "../../types/common";

interface DataTableProps<T> {
  columns: TableColumn<T>[];
  rows: T[];
}

export function DataTable<T>({ columns, rows }: DataTableProps<T>) {
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const totalPages = Math.max(1, Math.ceil(rows.length / pageSize));
  const startIndex = (currentPage - 1) * pageSize;
  const pageRows = useMemo(() => rows.slice(startIndex, startIndex + pageSize), [rows, startIndex, pageSize]);
  const visibleFrom = rows.length === 0 ? 0 : startIndex + 1;
  const visibleTo = Math.min(startIndex + pageRows.length, rows.length);

  useEffect(() => {
    if (currentPage > totalPages) {
      setCurrentPage(totalPages);
    }
  }, [currentPage, totalPages]);

  function getCellText(row: T, rowIndex: number, column: TableColumn<T>) {
    if (column.key === "stt") return String(rowIndex + 1);
    if (column.key === "actions") return "";
    return String((row as Record<string, unknown>)[column.key] ?? "");
  }

  function exportExcel() {
    const exportColumns = columns.filter((column) => column.key !== "actions");
    const headerHtml = exportColumns.map((column) => `<th>${escapeHtml(column.label)}</th>`).join("");
    const bodyHtml = rows
      .map((row, index) => `<tr>${exportColumns.map((column) => `<td>${escapeHtml(getCellText(row, index, column))}</td>`).join("")}</tr>`)
      .join("");
    const html = `<table><thead><tr>${headerHtml}</tr></thead><tbody>${bodyHtml}</tbody></table>`;
    const blob = new Blob(["\ufeff", html], { type: "application/vnd.ms-excel;charset=utf-8" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = `vehicle-management-${Date.now()}.xls`;
    link.click();
    URL.revokeObjectURL(url);
  }

  function exportPdf() {
    const exportColumns = columns.filter((column) => column.key !== "actions");
    const headerHtml = exportColumns.map((column) => `<th>${escapeHtml(column.label)}</th>`).join("");
    const bodyHtml = rows
      .map((row, index) => `<tr>${exportColumns.map((column) => `<td>${escapeHtml(getCellText(row, index, column))}</td>`).join("")}</tr>`)
      .join("");
    const printWindow = window.open("", "_blank", "width=1200,height=800");
    if (!printWindow) return;
    printWindow.document.write(`
      <html>
        <head>
          <title>Export PDF</title>
          <style>
            body { font-family: Arial, sans-serif; padding: 24px; color: #0f172a; }
            h1 { font-size: 20px; margin-bottom: 16px; }
            table { border-collapse: collapse; width: 100%; font-size: 12px; }
            th { background: #eff6ff; color: #1d4ed8; text-align: left; }
            th, td { border: 1px solid #cbd5e1; padding: 8px; }
            tr:nth-child(even) td { background: #f8fafc; }
          </style>
        </head>
        <body>
          <h1>Dữ liệu quản lý bãi xe</h1>
          <table><thead><tr>${headerHtml}</tr></thead><tbody>${bodyHtml}</tbody></table>
        </body>
      </html>
    `);
    printWindow.document.close();
    printWindow.focus();
    printWindow.print();
  }

  return (
    <>
      <div className="vm-table-toolbar">
        <div className="vm-table-length">
          <span>Hiển thị</span>
          <select
            className="form-control vm-form-control"
            value={pageSize}
            onChange={(event) => {
              setPageSize(Number(event.target.value));
              setCurrentPage(1);
            }}
          >
            {[5, 10, 20, 50].map((size) => <option value={size} key={size}>{size}</option>)}
          </select>
          <span>dòng</span>
        </div>
        <div className="vm-export-actions">
          <button type="button" className="btn vm-btn-export vm-btn-export-pdf" onClick={exportPdf}><i className="far fa-file-pdf" /> PDF</button>
          <button type="button" className="btn vm-btn-export vm-btn-export-excel" onClick={exportExcel}><i className="far fa-file-excel" /> Excel</button>
        </div>
      </div>
      <div className="table-responsive vm-table-shell">
        <table id="example1" className="table table-hover vm-data-table">
          <thead>
            <tr>
              {columns.map((column) => (
                <th key={column.key} style={{ width: column.width }} className={column.className}>{column.label}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {pageRows.map((row, rowIndex) => {
              const absoluteIndex = startIndex + rowIndex;
              return (
                <tr key={absoluteIndex}>
                  {columns.map((column) => (
                    <td key={column.key} className={column.className}>
                      {column.render ? column.render(row, absoluteIndex) : String((row as Record<string, unknown>)[column.key] ?? "")}
                    </td>
                  ))}
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
      <div className="vm-table-footer">
        <span>Hiển thị {visibleFrom} - {visibleTo} / {rows.length} dòng</span>
        <div className="vm-pagination">
          <button type="button" className="btn vm-page-btn" disabled={currentPage === 1} onClick={() => setCurrentPage((page) => Math.max(1, page - 1))}>
            <i className="fas fa-chevron-left" />
          </button>
          {Array.from({ length: totalPages }, (_, index) => index + 1).map((page) => (
            <button type="button" className={`btn vm-page-btn ${page === currentPage ? "active" : ""}`} key={page} onClick={() => setCurrentPage(page)}>
              {page}
            </button>
          ))}
          <button type="button" className="btn vm-page-btn" disabled={currentPage === totalPages} onClick={() => setCurrentPage((page) => Math.min(totalPages, page + 1))}>
            <i className="fas fa-chevron-right" />
          </button>
        </div>
      </div>
    </>
  );
}

function escapeHtml(value: string) {
  return value
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}
