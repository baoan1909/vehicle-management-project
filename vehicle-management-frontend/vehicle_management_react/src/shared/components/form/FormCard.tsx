import type { ReactNode } from "react";
import { AdminPage } from "../layout/AdminPage";

export interface FormField {
  label: string;
  name: string;
  type?: "text" | "email" | "password" | "number" | "date" | "select" | "checkbox" | "textarea";
  placeholder?: string;
  value?: string;
  options?: string[];
  checked?: boolean;
}

interface FormCardProps {
  title: string;
  breadcrumbs: { label: string; href?: string }[];
  cardTitle: string;
  fields: FormField[];
  backHref: string;
  extra?: ReactNode;
}

export function FormCard({ title, breadcrumbs, cardTitle, fields, backHref, extra }: FormCardProps) {
  return (
    <AdminPage title={title} breadcrumbs={breadcrumbs}>
      <div className="row d-flex justify-content-center mt-4">
        <div className="col-md-10">
          <div className="card card-cyan">
            <div className="card-header"><h3 className="card-title">{cardTitle}</h3></div>
            <form>
              <div className="card-body">
                {fields.map((field) => (
                  <div className="form-group" key={field.name}>
                    {field.type !== "checkbox" && <label>{field.label}</label>}
                    {field.type === "select" ? (
                      <select name={field.name} className="form-control select2" defaultValue={field.value ?? ""}>
                        {field.options?.map((option) => <option value={option} key={option}>{option}</option>)}
                      </select>
                    ) : field.type === "textarea" ? (
                      <textarea name={field.name} className="form-control" rows={4} defaultValue={field.value} placeholder={field.placeholder} />
                    ) : field.type === "checkbox" ? (
                      <div className="form-check">
                        <input type="checkbox" className="form-check-input" name={field.name} id={field.name} defaultChecked={field.checked} />
                        <label className="form-check-label" htmlFor={field.name}>{field.label}</label>
                      </div>
                    ) : (
                      <input type={field.type ?? "text"} name={field.name} className="form-control" defaultValue={field.value} placeholder={field.placeholder} />
                    )}
                  </div>
                ))}
                {extra}
              </div>
              <div className="card-footer">
                <a className="btn btn-default" href={backHref}>Thoát</a>
                <button type="button" className="btn btn-info float-right"><i className="fas fa-save" /> Lưu</button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </AdminPage>
  );
}
