import type { ReactNode } from "react";
import { Link } from "react-router-dom";
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
        <div className="col-xl-9 col-lg-10">
          <div className="card card-cyan vm-form-card tw-rounded-2xl tw-border tw-border-slate-100 tw-shadow-soft">
            <div className="card-header vm-card-header"><h3 className="card-title">{cardTitle}</h3></div>
            <form>
              <div className="card-body vm-form-body">
                {fields.map((field) => (
                  <div className="form-group vm-field" key={field.name}>
                    {field.type !== "checkbox" && <label className="vm-field-label">{field.label}</label>}
                    {field.type === "select" ? (
                      <select name={field.name} className="form-control select2 vm-form-control" defaultValue={field.value ?? ""}>
                        {field.options?.map((option) => <option value={option} key={option}>{option}</option>)}
                      </select>
                    ) : field.type === "textarea" ? (
                      <textarea name={field.name} className="form-control vm-form-control" rows={4} defaultValue={field.value} placeholder={field.placeholder} />
                    ) : field.type === "checkbox" ? (
                      <div className="form-check vm-checkbox">
                        <input type="checkbox" className="form-check-input" name={field.name} id={field.name} defaultChecked={field.checked} />
                        <label className="form-check-label" htmlFor={field.name}>{field.label}</label>
                      </div>
                    ) : (
                      <input type={field.type ?? "text"} name={field.name} className="form-control vm-form-control" defaultValue={field.value} placeholder={field.placeholder} />
                    )}
                  </div>
                ))}
                {extra}
              </div>
              <div className="card-footer vm-form-footer">
                <Link className="btn vm-btn-secondary" to={backHref}>Thoát</Link>
                <button type="button" className="btn vm-btn-primary float-right"><i className="fas fa-save" /> Lưu</button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </AdminPage>
  );
}
