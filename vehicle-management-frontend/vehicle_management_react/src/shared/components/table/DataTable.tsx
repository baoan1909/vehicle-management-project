import type { TableColumn } from "../../types/common";

interface DataTableProps<T> {
  columns: TableColumn<T>[];
  rows: T[];
}

export function DataTable<T>({ columns, rows }: DataTableProps<T>) {
  return (
    <table id="example1" className="table table-bordered table-striped">
      <thead>
        <tr>
          {columns.map((column) => (
            <th key={column.key} style={{ width: column.width }} className={column.className}>{column.label}</th>
          ))}
        </tr>
      </thead>
      <tbody>
        {rows.map((row, rowIndex) => (
          <tr key={rowIndex}>
            {columns.map((column) => (
              <td key={column.key} className={column.className}>
                {column.render ? column.render(row, rowIndex) : String((row as Record<string, unknown>)[column.key] ?? "")}
              </td>
            ))}
          </tr>
        ))}
      </tbody>
      <tfoot>
        <tr>
          {columns.map((column) => (
            <th key={column.key} style={{ width: column.width }} className={column.className}>{column.label}</th>
          ))}
        </tr>
      </tfoot>
    </table>
  );
}
