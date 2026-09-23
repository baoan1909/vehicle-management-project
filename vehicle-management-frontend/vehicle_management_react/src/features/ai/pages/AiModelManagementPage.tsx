import { useCallback, useEffect, useMemo, useState } from "react";
import { Button } from "@/components/ui";
import { useAuth } from "@/core/auth/useAuth";
import { hasAnyPermission } from "@/shared/auth/permissions";
import {
  getAiModelCatalog,
  getAiModelConfigurations,
  getAiModelWarnings,
  syncAiModelCatalog,
  updateAiModelRollout,
  updateAiModelStatus,
  type AiModelCatalogResponse,
  type AiModelConfigurationResponse,
  type AiModelStatus,
  type AiModelWarningResponse,
} from "@/features/ai/api/aiModelAdminApi";

const statusTone: Record<AiModelStatus, string> = {
  ACTIVE: "tw-bg-emerald-50 tw-text-emerald-700",
  CANDIDATE: "tw-bg-sky-50 tw-text-sky-700",
  DISABLED: "tw-bg-slate-100 tw-text-slate-600",
  FALLBACK: "tw-bg-amber-50 tw-text-amber-700",
};

export function AiModelManagementPage() {
  const { user } = useAuth();
  const canManage = hasAnyPermission(user, ["AI_MODEL_MANAGE_ALL"]);
  const canSyncCatalog = hasAnyPermission(user, ["AI_CATALOG_SYNC_ALL"]);

  const [catalog, setCatalog] = useState<AiModelCatalogResponse[]>([]);
  const [configs, setConfigs] = useState<AiModelConfigurationResponse[]>([]);
  const [warnings, setWarnings] = useState<AiModelWarningResponse[]>([]);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [savingId, setSavingId] = useState("");

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const [configResult, catalogResult, warningResult] = await Promise.all([
        getAiModelConfigurations(),
        getAiModelCatalog(),
        getAiModelWarnings(),
      ]);
      setConfigs(configResult.data ?? []);
      setCatalog(catalogResult.data ?? []);
      setWarnings(warningResult.data ?? []);
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Không thể tải dữ liệu AI model.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const catalogByModel = useMemo(() => new Map(catalog.map((model) => [`${model.provider}:${model.modelId}`, model])), [catalog]);
  const warningsByConfig = useMemo(() => {
    const grouped = new Map<string, AiModelWarningResponse[]>();
    warnings.forEach((warning) => {
      if (!warning.configurationId) return;
      grouped.set(warning.configurationId, [...(grouped.get(warning.configurationId) ?? []), warning]);
    });
    return grouped;
  }, [warnings]);

  async function changeStatus(config: AiModelConfigurationResponse, status: AiModelStatus) {
    if (!window.confirm(`Cập nhật ${config.modelId} sang ${status}?`)) return;
    setSavingId(config.configurationId);
    try {
      await updateAiModelStatus(config.configurationId, status);
      await load();
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Không thể cập nhật trạng thái model.");
    } finally {
      setSavingId("");
    }
  }

  async function changeRollout(config: AiModelConfigurationResponse, value: number) {
    setSavingId(config.configurationId);
    try {
      await updateAiModelRollout(config.configurationId, value);
      await load();
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Tổng rollout ACTIVE phải bằng 100.");
    } finally {
      setSavingId("");
    }
  }

  async function syncCatalog() {
    setSavingId("catalog");
    try {
      const response = await syncAiModelCatalog();
      setCatalog(response.data ?? []);
      await load();
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Không thể đồng bộ catalog.");
    } finally {
      setSavingId("");
    }
  }

  return (
    <main className="tw-grid tw-gap-4 tw-p-5 max-[700px]:tw-p-3">
      <section className="tw-flex tw-flex-wrap tw-items-center tw-justify-between tw-gap-3">
        <div>
          <h1 className="tw-m-0 tw-text-xl tw-font-black tw-text-slate-900">Quản trị model AI</h1>
          <p className="tw-m-0 tw-mt-1 tw-text-sm tw-font-semibold tw-text-slate-500">Gemini routing, rollout, catalog và cảnh báo vận hành.</p>
        </div>
        <Button disabled={!canSyncCatalog} loading={savingId === "catalog"} onClick={() => void syncCatalog()} variant="primary">Đồng bộ catalog</Button>
      </section>

      {error ? <div className="tw-rounded-lg tw-border tw-border-solid tw-border-red-200 tw-bg-red-50 tw-p-3 tw-text-sm tw-font-bold tw-text-red-700">{error}</div> : null}

      <section className="tw-overflow-hidden tw-rounded-lg tw-border tw-border-solid tw-border-slate-200 tw-bg-white">
        <div className="tw-overflow-x-auto">
          <table className="tw-min-w-[1100px] tw-w-full tw-border-collapse tw-text-sm">
            <thead className="tw-bg-slate-50 tw-text-left tw-text-xs tw-font-black tw-uppercase tw-text-slate-500">
              <tr>{["Model", "Use case", "Status", "Rollout", "Priority", "Capability", "Catalog", "Warnings", "Actions"].map((header) => <th className="tw-px-3 tw-py-3" key={header}>{header}</th>)}</tr>
            </thead>
            <tbody>
              {loading ? (
                <tr><td className="tw-p-6 tw-text-center tw-font-bold tw-text-slate-500" colSpan={9}>Đang tải...</td></tr>
              ) : configs.map((config) => {
                const model = catalogByModel.get(`${config.provider}:${config.modelId}`);
                const configWarnings = warningsByConfig.get(config.configurationId) ?? [];
                const busy = savingId === config.configurationId;
                return (
                  <tr className="tw-border-0 tw-border-t tw-border-solid tw-border-slate-100" key={config.configurationId}>
                    <td className="tw-px-3 tw-py-3"><b className="tw-block tw-text-slate-900">{config.modelId}</b><span className="tw-text-xs tw-font-semibold tw-text-slate-500">{config.provider} {config.apiVersion ?? ""}</span></td>
                    <td className="tw-px-3 tw-py-3 tw-font-bold tw-text-slate-700">{config.useCase}</td>
                    <td className="tw-px-3 tw-py-3"><span className={`tw-rounded tw-px-2 tw-py-1 tw-text-xs tw-font-black ${statusTone[config.status]}`}>{config.status}</span></td>
                    <td className="tw-px-3 tw-py-3"><input className="tw-w-20 tw-rounded tw-border tw-border-solid tw-border-slate-200 tw-px-2 tw-py-1" disabled={busy || !canManage} max={100} min={0} type="number" value={config.rolloutPercentage ?? 0} onChange={(event) => void changeRollout(config, Number(event.target.value))} /></td>
                    <td className="tw-px-3 tw-py-3">{config.priority ?? "-"}</td>
                    <td className="tw-px-3 tw-py-3 tw-text-xs tw-font-bold tw-text-slate-600">{config.requiresFunctionCalling ? "Function" : "Text"} / {config.requiresStructuredOutput ? "Structured" : "Free text"} / {config.freeTierApproved ? "Free tier OK" : "Paid only"}</td>
                    <td className="tw-px-3 tw-py-3 tw-text-xs tw-font-semibold tw-text-slate-600">{model ? (model.supportedActions.join(", ") || "No actions") : "Not seen"}</td>
                    <td className="tw-px-3 tw-py-3">{configWarnings.length ? <span className="tw-rounded tw-bg-red-50 tw-px-2 tw-py-1 tw-text-xs tw-font-black tw-text-red-700">{configWarnings.length} warning</span> : <span className="tw-text-xs tw-font-bold tw-text-emerald-600">OK</span>}</td>
                    <td className="tw-px-3 tw-py-3"><div className="tw-flex tw-flex-wrap tw-gap-1.5"><button className="tw-rounded tw-border tw-border-solid tw-border-emerald-200 tw-bg-white tw-px-2 tw-py-1 tw-text-xs tw-font-black tw-text-emerald-700 disabled:tw-opacity-50" disabled={busy || !canManage || config.status === "ACTIVE"} onClick={() => void changeStatus(config, "ACTIVE")} type="button">Active</button><button className="tw-rounded tw-border tw-border-solid tw-border-amber-200 tw-bg-white tw-px-2 tw-py-1 tw-text-xs tw-font-black tw-text-amber-700 disabled:tw-opacity-50" disabled={busy || !canManage || config.status === "FALLBACK"} onClick={() => void changeStatus(config, "FALLBACK")} type="button">Fallback</button><button className="tw-rounded tw-border tw-border-solid tw-border-slate-200 tw-bg-white tw-px-2 tw-py-1 tw-text-xs tw-font-black tw-text-slate-600 disabled:tw-opacity-50" disabled={busy || !canManage || config.status === "DISABLED"} onClick={() => void changeStatus(config, "DISABLED")} type="button">Disable</button></div></td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </section>
    </main>
  );
}
