import { forwardRef, useEffect, useImperativeHandle, useRef, useState, type ChangeEvent, type DragEvent } from "react";

import { Card, CardContent, CardHeader } from "@/components/ui";
import { cn } from "@/lib/cn";

type CaptureResult = {
  file: File;
  url: string;
};

type OcrStatus = "idle" | "recognizing" | "success" | "review" | "error";

const LANE_IMAGE_MAX_EDGE = 1280;
const LANE_IMAGE_JPEG_QUALITY = 0.86;

type CameraCaptureBoxProps = {
  compact?: boolean;
  defaultCaptured?: boolean;
  label: string;
  onCaptureChange?: (capture: CaptureResult | null) => void;
  restoredImageUrl?: string;
  scene: "lane" | "driver";
};

export type CameraCaptureBoxHandle = {
  captureFrame: () => boolean;
  clear: (restartCamera?: boolean) => void;
  startCamera: () => Promise<void>;
  uploadFile: (file: File) => void;
};

function CameraLoadingScene({ compact }: { compact?: boolean }) {
  return (
    <div className="tw-relative tw-flex tw-h-full tw-w-full tw-items-center tw-justify-center tw-overflow-hidden tw-bg-[linear-gradient(135deg,#0f5fd8_0%,#2563eb_48%,#0ea5e9_100%)]">
      <div className="tw-absolute tw-inset-0 tw-bg-[radial-gradient(circle_at_28%_18%,rgba(255,255,255,0.26),transparent_32%),radial-gradient(circle_at_74%_70%,rgba(14,165,233,0.42),transparent_34%)]" />
      <div className="tw-absolute tw-inset-0 tw-bg-[linear-gradient(90deg,rgba(255,255,255,0.08)_1px,transparent_1px),linear-gradient(0deg,rgba(255,255,255,0.08)_1px,transparent_1px)] tw-bg-[length:34px_34px]" />
      <div className={cn("tw-relative tw-flex tw-items-center tw-justify-center", compact ? "tw-h-[58px] tw-w-[58px]" : "tw-h-[76px] tw-w-[76px]")}>
        <span className="tw-absolute tw-inset-0 tw-animate-spin tw-rounded-full tw-border-[4px] tw-border-solid tw-border-white/30 tw-border-t-white" />
        <span className="tw-absolute tw-inset-[9px] tw-rounded-full tw-bg-white/15 tw-shadow-[inset_0_0_0_1px_rgba(255,255,255,0.22)]" />
        <span className={cn("tw-relative tw-inline-flex tw-items-center tw-justify-center tw-rounded-full tw-bg-white tw-text-vm-primary tw-shadow-[0_16px_34px_rgba(15,23,42,0.18)]", compact ? "tw-h-8 tw-w-8" : "tw-h-10 tw-w-10")}>
          <i className={cn(sceneIconClass(compact), "fas fa-video")} />
        </span>
      </div>
    </div>
  );
}

function sceneIconClass(compact?: boolean) {
  return compact ? "tw-text-[0.9rem]" : "tw-text-[1.08rem]";
}

const CameraCaptureBox = forwardRef<CameraCaptureBoxHandle, CameraCaptureBoxProps>(function CameraCaptureBox(
  { compact, defaultCaptured, label, onCaptureChange, restoredImageUrl, scene },
  ref,
) {
  const [capture, setCapture] = useState<CaptureResult | null>(null);
  const [isCapturing, setIsCapturing] = useState(false);
  const [stream, setStream] = useState<MediaStream | null>(null);
  const [cameraError, setCameraError] = useState("");
  const [restoredImageDismissed, setRestoredImageDismissed] = useState(false);
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const canvasRef = useRef<HTMLCanvasElement | null>(null);

  useEffect(() => {
    if (!defaultCaptured || scene !== "driver") return undefined;
    setCapture(null);
    onCaptureChange?.(null);
    return undefined;
  }, [defaultCaptured, scene]);

  useEffect(() => {
    setRestoredImageDismissed(false);
  }, [restoredImageUrl]);

  useEffect(() => {
    if (videoRef.current && stream) {
      videoRef.current.srcObject = stream;
    }
  }, [stream]);

  useEffect(() => {
    return () => {
      stream?.getTracks().forEach((track) => track.stop());
      if (capture?.url) URL.revokeObjectURL(capture.url);
    };
  }, [capture?.url, stream]);

  async function startCamera(force = false) {
    if (!force && restoredImageUrl && !restoredImageDismissed) return;
    if (stream || isCapturing) return;
    try {
      setCameraError("");
      const mediaStream = await navigator.mediaDevices.getUserMedia({
        video: {
          width: { ideal: 1280 },
          height: { ideal: 720 },
          facingMode: scene === "driver" ? "user" : "environment",
        },
      });
      setStream(mediaStream);
      setIsCapturing(true);
    } catch {
      setCameraError("Không thể mở camera");
      setIsCapturing(false);
    }
  }

  function stopCamera() {
    stream?.getTracks().forEach((track) => track.stop());
    setStream(null);
    setIsCapturing(false);
  }

  function clearCapture(restartCamera = true) {
    if (capture?.url) URL.revokeObjectURL(capture.url);
    setCapture(null);
    setRestoredImageDismissed(true);
    onCaptureChange?.(null);
    setCameraError("");
    if (restartCamera) void startCamera(true);
  }

  function uploadFile(file: File) {
    if (capture?.url) URL.revokeObjectURL(capture.url);
    const nextCapture = { file, url: URL.createObjectURL(file) };
    setCapture(nextCapture);
    onCaptureChange?.(nextCapture);
    stopCamera();
    setCameraError("");
  }

  useImperativeHandle(ref, () => ({
    captureFrame,
    clear: clearCapture,
    startCamera,
    uploadFile,
  }));

  function captureFrame() {
    const video = videoRef.current;
    const canvas = canvasRef.current;
    const context = canvas?.getContext("2d");
    if (!video || !canvas || !context || !video.videoWidth || !video.videoHeight) return false;

    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    context.drawImage(video, 0, 0);
    canvas.toBlob(
      (blob) => {
        if (!blob) return;
        if (capture?.url) URL.revokeObjectURL(capture.url);
        const url = URL.createObjectURL(blob);
        const file = new File([blob], `${scene}_${Date.now()}.jpg`, { type: blob.type });
        const nextCapture = { file, url };
        setCapture(nextCapture);
        onCaptureChange?.(nextCapture);
        stopCamera();
      },
      "image/jpeg",
      0.86,
    );
    return true;
  }

  const visibleRestoredImageUrl = restoredImageDismissed ? "" : restoredImageUrl;
  const hasVisualCapture = capture || visibleRestoredImageUrl || defaultCaptured;
  const captureStatusLabel = isCapturing
    ? "LIVE"
    : capture
      ? "Đã chụp"
      : visibleRestoredImageUrl
        ? "Đã lưu"
        : defaultCaptured
          ? "Ảnh mẫu"
          : "Chưa chụp";

  return (
    <div className={cn("tw-relative tw-overflow-hidden tw-rounded-vm-md tw-border tw-border-solid", compact ? "tw-h-[180px]" : "tw-h-[286px]", hasVisualCapture ? "tw-border-vm-slate-100" : "tw-border-vm-slate-200")}>
      {isCapturing ? (
        <video ref={videoRef} autoPlay playsInline muted className="tw-h-full tw-w-full tw-bg-slate-900 tw-object-cover" />
      ) : capture ? (
        <img src={capture.url} alt={label} className="tw-h-full tw-w-full tw-object-cover" />
      ) : visibleRestoredImageUrl ? (
        <img src={visibleRestoredImageUrl} alt={label} className="tw-h-full tw-w-full tw-object-cover" />
      ) : (
        <CameraLoadingScene compact={compact} />
      )}

      <canvas ref={canvasRef} className="tw-hidden" />

      {scene === "lane" ? (
        <>
          <span className="tw-absolute tw-left-3 tw-top-3 tw-inline-flex tw-items-center tw-gap-1.5 tw-rounded-vm-sm tw-border tw-border-solid tw-border-white/40 tw-bg-slate-900/70 tw-px-2 tw-py-1 tw-text-[0.72rem] tw-font-black tw-text-white">
            <span className="tw-h-2 tw-w-2 tw-rounded-full tw-bg-emerald-400" />
            LIVE
          </span>
          <button className="tw-absolute tw-right-3 tw-top-3 tw-inline-flex tw-h-9 tw-w-9 tw-items-center tw-justify-center tw-rounded-vm-sm tw-border tw-border-solid tw-border-white/40 tw-bg-slate-900/50 tw-text-white" type="button" aria-label="Phóng to camera">
            <i className="fas fa-expand" />
          </button>
        </>
      ) : null}

      {compact ? (
        <span className="tw-absolute tw-left-2 tw-top-2 tw-rounded-full tw-bg-emerald-50 tw-px-2 tw-py-1 tw-text-[0.68rem] tw-font-extrabold tw-text-emerald-700">
          {captureStatusLabel}
        </span>
      ) : null}

      {cameraError ? (
        <span className="tw-absolute tw-bottom-3 tw-left-3 tw-right-3 tw-rounded-vm-sm tw-bg-red-50 tw-px-3 tw-py-2 tw-text-center tw-text-[0.76rem] tw-font-bold tw-text-red-600">
          {cameraError}
        </span>
      ) : null}

    </div>
  );
});

function CameraAction({ icon, label, onClick }: { icon: string; label: string; onClick?: () => void }) {
  return (
    <button
      type="button"
      className="tw-flex tw-h-9 tw-items-center tw-justify-center tw-gap-2.5 tw-border-0 tw-border-r tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-text-[0.82rem] tw-font-bold tw-text-vm-slate-700 tw-transition last:tw-border-r-0 hover:tw-bg-brand-50 hover:tw-text-vm-primary"
      onClick={onClick}
    >
      <i className={cn(icon, "tw-text-vm-primary")} />
      {label}
    </button>
  );
}

function loadImage(url: string) {
  return new Promise<HTMLImageElement>((resolve, reject) => {
    const image = new Image();
    image.onload = () => resolve(image);
    image.onerror = reject;
    image.src = url;
  });
}

async function normalizeLaneImageFile(file: File) {
  if (!file.type.startsWith("image/")) return file;

  const objectUrl = URL.createObjectURL(file);
  try {
    const image = await loadImage(objectUrl);
    const maxEdge = Math.max(image.naturalWidth, image.naturalHeight);
    const scale = maxEdge > LANE_IMAGE_MAX_EDGE ? LANE_IMAGE_MAX_EDGE / maxEdge : 1;
    const width = Math.max(1, Math.round(image.naturalWidth * scale));
    const height = Math.max(1, Math.round(image.naturalHeight * scale));
    const canvas = document.createElement("canvas");
    const context = canvas.getContext("2d");
    if (!context) return file;

    canvas.width = width;
    canvas.height = height;
    context.drawImage(image, 0, 0, width, height);

    const blob = await new Promise<Blob | null>((resolve) => {
      canvas.toBlob(resolve, "image/jpeg", LANE_IMAGE_JPEG_QUALITY);
    });
    if (!blob) return file;

    return new File([blob], `lane_${Date.now()}.jpg`, { type: "image/jpeg" });
  } catch {
    return file;
  } finally {
    URL.revokeObjectURL(objectUrl);
  }
}

type ParkingCameraPanelProps = {
  autoCaptureKey?: number;
  autoStartLaneCamera?: boolean;
  canCaptureMedia?: boolean;
  ocrMessage?: string;
  ocrStatus?: OcrStatus;
  resetKey?: number;
  restoredLicensePlateImageUrl?: string;
  restoredPersonImageUrl?: string;
  onAutoCaptureFailed?: () => void;
  onLicensePlateImageChange: (file: File | null) => void;
  onPersonImageChange: (file: File | null) => void;
  onRequireCardBeforeCapture?: () => void;
};

function OcrCameraStatus({ message, status }: { message?: string; status: OcrStatus }) {
  if (status === "idle" || !message) return null;

  const toneClassName =
    status === "success"
      ? "tw-border-emerald-100 tw-bg-emerald-50 tw-text-emerald-700"
      : status === "recognizing"
        ? "tw-border-brand-100 tw-bg-brand-50 tw-text-vm-primary"
        : status === "review"
          ? "tw-border-amber-100 tw-bg-amber-50 tw-text-amber-700"
          : "tw-border-red-100 tw-bg-red-50 tw-text-red-600";
  const icon =
    status === "success"
      ? "fas fa-check-circle"
      : status === "recognizing"
        ? "fas fa-spinner fa-spin"
        : status === "review"
          ? "fas fa-exclamation-triangle"
          : "fas fa-info-circle";

  return (
    <div className={cn("tw-mx-3 tw-mt-2 tw-flex tw-min-h-9 tw-items-center tw-gap-2 tw-rounded-vm-md tw-border tw-border-solid tw-px-3 tw-py-2 tw-text-[0.78rem] tw-font-extrabold", toneClassName)}>
      <i className={cn(icon, "tw-w-4 tw-text-center")} />
      <span className="tw-min-w-0 tw-flex-1">{message}</span>
    </div>
  );
}

export function ParkingCameraPanel({
  autoCaptureKey = 0,
  autoStartLaneCamera = false,
  canCaptureMedia = true,
  ocrMessage,
  ocrStatus = "idle",
  resetKey = 0,
  restoredLicensePlateImageUrl,
  restoredPersonImageUrl,
  onAutoCaptureFailed,
  onLicensePlateImageChange,
  onPersonImageChange,
  onRequireCardBeforeCapture,
}: ParkingCameraPanelProps) {
  const laneCameraRef = useRef<CameraCaptureBoxHandle | null>(null);
  const driverCameraRef = useRef<CameraCaptureBoxHandle | null>(null);
  const laneUploadRef = useRef<HTMLInputElement | null>(null);

  useEffect(() => {
    if (!autoStartLaneCamera) return;
    void laneCameraRef.current?.startCamera();
    void driverCameraRef.current?.startCamera();
  }, [autoStartLaneCamera]);

  useEffect(() => {
    if (!resetKey) return;
    laneCameraRef.current?.clear(true);
    driverCameraRef.current?.clear(true);
  }, [resetKey]);

  useEffect(() => {
    if (!autoCaptureKey) return undefined;

    let cancelled = false;
    let attempt = 0;
    let timeoutId: number | undefined;

    const tryCapture = () => {
      if (cancelled) return;
      const captured = laneCameraRef.current?.captureFrame() ?? false;
      if (captured) return;
      if (attempt >= 8) {
        onAutoCaptureFailed?.();
        return;
      }
      attempt += 1;
      timeoutId = window.setTimeout(tryCapture, 250);
    };

    void laneCameraRef.current?.startCamera().finally(() => {
      timeoutId = window.setTimeout(tryCapture, 300);
    });

    return () => {
      cancelled = true;
      if (timeoutId) window.clearTimeout(timeoutId);
    };
  }, [autoCaptureKey, onAutoCaptureFailed]);

  function handleReloadLaneCamera() {
    laneCameraRef.current?.clear();
  }

  function ensureCanCaptureMedia() {
    if (canCaptureMedia) return true;
    onRequireCardBeforeCapture?.();
    return false;
  }

  function handleCaptureLanePhoto() {
    if (!ensureCanCaptureMedia()) return;

    let attempt = 0;
    const tryCapture = () => {
      const captured = laneCameraRef.current?.captureFrame() ?? false;
      if (captured) return;
      if (attempt >= 8) {
        onAutoCaptureFailed?.();
        return;
      }
      attempt += 1;
      window.setTimeout(tryCapture, 250);
    };

    void laneCameraRef.current?.startCamera().finally(() => {
      window.setTimeout(tryCapture, 120);
    });
  }

  function handleCaptureDriverPhoto() {
    if (!ensureCanCaptureMedia()) return;

    let attempt = 0;
    const tryCapture = () => {
      const captured = driverCameraRef.current?.captureFrame() ?? false;
      if (captured || attempt >= 8) return;
      attempt += 1;
      window.setTimeout(tryCapture, 250);
    };

    void driverCameraRef.current?.startCamera().finally(() => {
      window.setTimeout(tryCapture, 120);
    });
  }

  function handleReloadDriverCamera() {
    if (!ensureCanCaptureMedia()) return;
    driverCameraRef.current?.clear();
  }

  async function handleLaneFile(file: File) {
    if (!ensureCanCaptureMedia()) return;

    const normalizedFile = await normalizeLaneImageFile(file);
    if (laneCameraRef.current) {
      laneCameraRef.current.uploadFile(normalizedFile);
      return;
    }
    onLicensePlateImageChange(normalizedFile);
  }

  function handleUploadChange(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    if (!file) return;
    void handleLaneFile(file);
    event.target.value = "";
  }

  function handleDrop(event: DragEvent<HTMLDivElement>) {
    event.preventDefault();
    const file = Array.from(event.dataTransfer.files).find((item) => item.type.startsWith("image/"));
    if (file) void handleLaneFile(file);
  }

  return (
    <Card className="tw-flex tw-min-h-0 tw-flex-col tw-overflow-hidden">
      <CardHeader className="tw-flex tw-min-h-[50px] tw-items-center tw-justify-between tw-px-4 tw-py-0">
        <h2 className="tw-m-0 tw-text-[1rem] tw-font-extrabold tw-text-slate-900">Camera làn xe</h2>
        <span className="tw-inline-flex tw-items-center tw-gap-2 tw-rounded-full tw-border tw-border-solid tw-border-emerald-200 tw-bg-emerald-50 tw-px-2.5 tw-py-1 tw-text-[0.72rem] tw-font-extrabold tw-text-emerald-700">
          <span className="tw-h-2 tw-w-2 tw-rounded-full tw-bg-emerald-500" />
          CAM-A1-01
        </span>
      </CardHeader>

      <CardContent className="tw-flex tw-min-h-0 tw-flex-1 tw-flex-col tw-p-0">
        <div
          className="tw-p-3 tw-pb-0"
          onDragOver={(event) => event.preventDefault()}
          onDrop={handleDrop}
        >
          <CameraCaptureBox
            ref={laneCameraRef}
            label="Ảnh biển số"
            restoredImageUrl={restoredLicensePlateImageUrl}
            scene="lane"
            onCaptureChange={(capture) => onLicensePlateImageChange(capture?.file ?? null)}
          />
        </div>
        <div className="tw-mt-2 tw-grid tw-grid-cols-3 tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-px-3 tw-py-0">
          <CameraAction icon="fas fa-sync-alt" label="Load lại" onClick={handleReloadLaneCamera} />
          <CameraAction icon="fas fa-camera" label="Chụp ảnh" onClick={handleCaptureLanePhoto} />
          <CameraAction icon="fas fa-cloud-upload-alt" label="Tải ảnh lên" onClick={() => laneUploadRef.current?.click()} />
        </div>
        <input
          ref={laneUploadRef}
          className="tw-hidden"
          type="file"
          accept="image/*"
          onClick={(event) => {
            if (!ensureCanCaptureMedia()) event.preventDefault();
          }}
          onChange={handleUploadChange}
        />
        <OcrCameraStatus message={ocrMessage} status={ocrStatus} />

        <div className="tw-min-h-0 tw-flex-1 tw-p-3">
          <div className="tw-mb-2 tw-flex tw-items-center tw-gap-2">
            <h3 className="tw-m-0 tw-text-[0.92rem] tw-font-extrabold tw-text-slate-900">Ảnh người / tài xế</h3>
            <span className="tw-inline-flex tw-items-center tw-gap-1.5 tw-rounded-full tw-bg-emerald-50 tw-px-2.5 tw-py-1 tw-text-[0.7rem] tw-font-extrabold tw-text-emerald-700">
              <span className="tw-h-1.5 tw-w-1.5 tw-rounded-full tw-bg-emerald-500" />
              Ảnh bắt buộc
            </span>
          </div>
          <div className="tw-grid tw-grid-cols-[minmax(0,1fr)_132px] tw-gap-3 max-[1280px]:tw-grid-cols-1">
            <CameraCaptureBox
              ref={driverCameraRef}
              compact
              defaultCaptured
              label="Ảnh người / tài xế"
              restoredImageUrl={restoredPersonImageUrl}
              scene="driver"
              onCaptureChange={(capture) => onPersonImageChange(capture?.file ?? null)}
            />
            <div className="tw-flex tw-min-h-[180px] tw-flex-col tw-justify-center tw-gap-2">
              <button
                type="button"
                className="tw-flex tw-h-[62px] tw-items-center tw-justify-center tw-gap-2 tw-rounded-vm-md tw-border tw-border-dashed tw-border-vm-slate-200 tw-bg-white tw-text-[0.8rem] tw-font-bold tw-text-vm-slate-700 tw-transition hover:tw-border-brand-200 hover:tw-bg-brand-50 hover:tw-text-vm-primary"
                onClick={handleReloadDriverCamera}
              >
                <span className="tw-inline-flex tw-h-8 tw-w-8 tw-items-center tw-justify-center tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-200 tw-text-[0.95rem]">
                  <i className="fas fa-sync-alt" />
                </span>
                Chụp lại
              </button>
              <button
                type="button"
                className="tw-flex tw-h-[62px] tw-items-center tw-justify-center tw-gap-2 tw-rounded-vm-md tw-border tw-border-solid tw-border-brand-100 tw-bg-brand-50 tw-text-[0.8rem] tw-font-extrabold tw-text-vm-primary tw-transition hover:tw-border-brand-200 hover:tw-bg-brand-100"
                onClick={handleCaptureDriverPhoto}
              >
                <span className="tw-inline-flex tw-h-8 tw-w-8 tw-items-center tw-justify-center tw-rounded-vm-md tw-border tw-border-solid tw-border-brand-200 tw-bg-white tw-text-[0.95rem]">
                  <i className="fas fa-camera" />
                </span>
                Chụp ảnh
              </button>
            </div>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
