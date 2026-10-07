import { Component, useCallback, useEffect, useMemo, useRef, useState, type ErrorInfo, type ReactNode } from "react";

import {
  getNearbyParkingLots,
  getParkingMapFeatures,
  reversePublicParkingLocation,
  recordGeolocationOutcome,
  searchPublicParkingLocations,
  type NearbyParkingLot,
  type ParkingMapFeatureStatus,
} from "@/features/parking/api/publicParkingApi";
import {
  NearbyParkingMap,
  type MapPoint,
} from "@/features/parking/components/NearbyParkingMap";
import {
  ParkingAddressPicker,
  type ParkingAddressValue,
} from "@/features/parking/components/ParkingAddressPicker";

const RADIUS_OPTIONS = [1, 3, 5] as const;
const LOW_ACCURACY_THRESHOLD_METERS = 500;

type MapErrorBoundaryProps = {
  children: ReactNode;
  onError: () => void;
};

class MapErrorBoundary extends Component<MapErrorBoundaryProps, { hasError: boolean }> {
  state = { hasError: false };

  static getDerivedStateFromError() {
    return { hasError: true };
  }

  componentDidCatch(_error: Error, _info: ErrorInfo) {
    this.props.onError();
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="tw-grid tw-h-full tw-min-h-[360px] tw-place-items-center tw-bg-slate-50 tw-p-8 tw-text-center">
          <div>
            <i className="fas fa-map-marked-alt tw-mb-3 tw-text-3xl tw-text-slate-400" aria-hidden="true" />
            <p className="tw-m-0 tw-font-extrabold tw-text-slate-800">Bản đồ chưa thể tải</p>
            <p className="tw-mt-2 tw-text-sm tw-text-slate-600">Bạn vẫn có thể chọn bãi trong danh sách và mở chỉ đường.</p>
          </div>
        </div>
      );
    }
    return this.props.children;
  }
}

function formatDistance(lot: NearbyParkingLot) {
  if (lot.distanceMeters < 1000) return `${Math.round(lot.distanceMeters)} m`;
  return `${Number(lot.distanceKm).toFixed(2)} km`;
}

function createDirectionsUrl(origin: MapPoint, destination: NearbyParkingLot) {
  const url = new URL("https://www.google.com/maps/dir/");
  url.search = new URLSearchParams({
    api: "1",
    origin: `${origin.latitude},${origin.longitude}`,
    destination: `${destination.latitude},${destination.longitude}`,
    travelmode: "driving",
  }).toString();
  return url.toString();
}

export function NearbyParkingLotsPage() {
  const [featureStatus, setFeatureStatus] = useState<ParkingMapFeatureStatus | null>(null);
  const [featureStatusError, setFeatureStatusError] = useState(false);  const [radiusKm, setRadiusKm] = useState<(typeof RADIUS_OPTIONS)[number]>(5);
  const [userLocation, setUserLocation] = useState<MapPoint | null>(null);
  const [locationLabel, setLocationLabel] = useState("");
  const [lots, setLots] = useState<NearbyParkingLot[]>([]);
  const [selectedLotId, setSelectedLotId] = useState<string | null>(null);
  const [isLocating, setIsLocating] = useState(false);
  const [isSearching, setIsSearching] = useState(false);
  const [isGeocoding, setIsGeocoding] = useState(false);
  const [showFallback, setShowFallback] = useState(false);
  const [pickingEnabled, setPickingEnabled] = useState(false);
  const [mapFailed, setMapFailed] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [address, setAddress] = useState<ParkingAddressValue>({
    addressDisplay: "",
    addressInputScheme: "CURRENT",
    currentWardCode: null,
    legacyWardCode: null,
  });
  const searchSequence = useRef(0);

  useEffect(() => {
    let active = true;
    void getParkingMapFeatures()
      .then((response) => {
        if (active) setFeatureStatus(response.data);
      })
      .catch(() => {
        if (active) setFeatureStatusError(true);
      });
    return () => {
      active = false;
    };
  }, []);
  const selectedLot = useMemo(
    () => lots.find((lot) => lot.parkingLotId === selectedLotId) ?? null,
    [lots, selectedLotId],
  );

  const searchNearby = useCallback(async (point: MapPoint, nextRadiusKm = radiusKm) => {
    const sequence = ++searchSequence.current;
    setIsSearching(true);
    setError(null);
    try {
      const response = await getNearbyParkingLots(point.latitude, point.longitude, nextRadiusKm, 50);
      if (sequence !== searchSequence.current) return;
      setLots(response.data);
      setSelectedLotId(null);
    } catch (caught) {
      if (sequence !== searchSequence.current) return;
      setLots([]);
      setSelectedLotId(null);
      setError(caught instanceof Error ? caught.message : "Không thể tìm bãi xe lúc này. Vui lòng thử lại.");
    } finally {
      if (sequence === searchSequence.current) setIsSearching(false);
    }
  }, [radiusKm]);

  function handleFindNearMe() {
    if (!featureStatus?.customerParkingMapEnabled || !featureStatus.publicNearbySearchEnabled) {
      setError("Bản đồ tìm bãi hiện chưa được bật.");
      return;
    }
    setNotice(null);
    setError(null);
    setPickingEnabled(false);

    if (import.meta.env.PROD && !window.isSecureContext) {
      void recordGeolocationOutcome("INSECURE_CONTEXT").catch(() => undefined);
      setError("Định vị trên production chỉ hoạt động qua HTTPS. Hãy dùng phần chọn địa chỉ hoặc chọn điểm trên bản đồ.");
      setShowFallback(true);
      return;
    }
    if (!("geolocation" in navigator)) {
      void recordGeolocationOutcome("UNSUPPORTED").catch(() => undefined);
      setError("Trình duyệt này không hỗ trợ định vị. Hãy chọn địa chỉ hoặc một điểm trên bản đồ.");
      setShowFallback(true);
      return;
    }

    setIsLocating(true);
    navigator.geolocation.getCurrentPosition(
      (position) => {
        const point: MapPoint = {
          latitude: position.coords.latitude,
          longitude: position.coords.longitude,
          accuracy: position.coords.accuracy,
        };
        setIsLocating(false);
        void recordGeolocationOutcome("GRANTED").catch(() => undefined);
        setUserLocation(point);
        setLocationLabel("Vị trí hiện tại của bạn");
        if (position.coords.accuracy > LOW_ACCURACY_THRESHOLD_METERS) {
          void recordGeolocationOutcome("LOW_ACCURACY").catch(() => undefined);
          setNotice(`Vị trí có độ chính xác thấp (khoảng ${Math.round(position.coords.accuracy)} m). Bạn có thể chọn lại điểm trên bản đồ.`);
        }
        void searchNearby(point);
      },
      (geolocationError) => {
        setIsLocating(false);
        setShowFallback(true);
        if (geolocationError.code === geolocationError.PERMISSION_DENIED) {
          void recordGeolocationOutcome("DENIED").catch(() => undefined);
          setError("Bạn đã từ chối quyền vị trí. Hãy cho phép GPS trong trình duyệt hoặc dùng cách tìm thủ công bên dưới.");
        } else if (geolocationError.code === geolocationError.TIMEOUT) {
          void recordGeolocationOutcome("TIMEOUT").catch(() => undefined);
          setError("Yêu cầu lấy vị trí đã hết thời gian. Hãy thử lại hoặc chọn địa chỉ.");
        } else {
          void recordGeolocationOutcome("UNAVAILABLE").catch(() => undefined);
          setError("Không lấy được vị trí. GPS có thể đang tắt hoặc tín hiệu chưa sẵn sàng.");
        }
      },
      { enableHighAccuracy: true, maximumAge: 0, timeout: 12_000 },
    );
  }

  async function handleAddressSearch() {
    if (!featureStatus?.geocodingProviderEnabled) {
      setError("Tra cứu tọa độ theo địa chỉ đang tạm dừng. Bạn vẫn có thể chọn điểm trên bản đồ.");
      return;
    }
    if (address.addressDisplay.trim().length < 3) {
      setError("Hãy chọn khu vực hoặc nhập địa chỉ có ít nhất 3 ký tự.");
      return;
    }
    setIsGeocoding(true);
    setError(null);
    setNotice(null);
    try {
      const response = await searchPublicParkingLocations(address.addressDisplay);
      const location = response.data[0];
      if (!location) {
        setError("Không tìm thấy tọa độ cho địa chỉ này. Bạn có thể chọn điểm trực tiếp trên bản đồ.");
        return;
      }
      const point = { latitude: location.latitude, longitude: location.longitude };
      setUserLocation(point);
      setLocationLabel(location.displayName);
      setPickingEnabled(false);
      await searchNearby(point);
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Không thể tìm tọa độ cho địa chỉ này.");
    } finally {
      setIsGeocoding(false);
    }
  }

  function handlePickLocation(point: MapPoint) {
    setUserLocation(point);
    setLocationLabel("Điểm bạn chọn trên bản đồ");
    setPickingEnabled(false);
    setError(null);
    setNotice("Đã chọn điểm trên bản đồ và đang tìm bãi xe gần đó.");
    void searchNearby(point);
    if (featureStatus?.geocodingProviderEnabled) {
      void reversePublicParkingLocation(point.latitude, point.longitude)
        .then((response) => {
          if (response.data[0]) setLocationLabel(response.data[0].displayName);
        })
        .catch(() => undefined);
    }
  }

  function handleRadiusChange(nextRadius: (typeof RADIUS_OPTIONS)[number]) {
    setRadiusKm(nextRadius);
    if (userLocation) void searchNearby(userLocation, nextRadius);
  }

  if (!featureStatus && !featureStatusError) {
    return (
      <main className="tw-grid tw-min-h-[calc(100vh-72px)] tw-place-items-center tw-bg-slate-50 tw-p-6" role="status">
        <p className="tw-font-extrabold tw-text-slate-700">Đang kiểm tra trạng thái bản đồ...</p>
      </main>
    );
  }

  if (
    featureStatusError ||
    !featureStatus?.customerParkingMapEnabled ||
    !featureStatus.publicNearbySearchEnabled
  ) {
    return (
      <main className="tw-grid tw-min-h-[calc(100vh-72px)] tw-place-items-center tw-bg-slate-50 tw-p-6">
        <section className="tw-max-w-xl tw-rounded-vm-lg tw-border tw-border-solid tw-border-slate-200 tw-bg-white tw-p-8 tw-text-center tw-shadow-sm" role="status">
          <i className="fas fa-map-marked-alt tw-text-4xl tw-text-slate-300" aria-hidden="true" />
          <h1 className="tw-mb-2 tw-mt-4 tw-text-2xl tw-font-black tw-text-slate-950">Bản đồ tìm bãi hiện chưa được bật</h1>
          <p className="tw-m-0 tw-text-slate-600">Hệ thống đang triển khai theo từng giai đoạn. Vui lòng quay lại sau.</p>
        </section>
      </main>
    );
  }
  return (
    <main className="tw-min-h-[calc(100vh-72px)] tw-bg-slate-50 tw-px-4 tw-py-8 sm:tw-px-6 lg:tw-px-8">
      <div className="tw-mx-auto tw-max-w-[1440px]">
        <section
          className="tw-overflow-hidden tw-rounded-[28px] tw-p-6 tw-text-white tw-shadow-xl sm:tw-p-9"
          style={{ background: "linear-gradient(to bottom right, #020617, #172554, #1D4ED8)" }}
        >
          <div className="tw-max-w-3xl">
            <span className="tw-inline-flex tw-items-center tw-gap-2 tw-rounded-full tw-bg-white/10 tw-px-3 tw-py-1 tw-text-xs tw-font-extrabold tw-uppercase tw-tracking-wider">
              <i className="fas fa-location-arrow" aria-hidden="true" /> Tìm bãi xe
            </span>
            <h1 className="tw-mb-3 tw-mt-4 tw-text-3xl tw-font-black tw-leading-tight sm:tw-text-5xl">Tìm bãi xe gần bạn trong vài giây</h1>
            <p className="tw-mb-6 tw-max-w-2xl tw-text-sm tw-leading-6 tw-text-blue-100 sm:tw-text-base">
              Vị trí chỉ được dùng cho lần tìm kiếm hiện tại, không lưu vào trình duyệt và không tự động thu thập khi bạn mở trang.
            </p>
            <button
              className="tw-inline-flex tw-min-h-12 tw-items-center tw-justify-center tw-gap-2 tw-rounded-full tw-border-0 tw-bg-white tw-px-6 tw-font-black tw-text-blue-800 tw-shadow-lg tw-transition hover:tw-bg-blue-50 disabled:tw-cursor-wait disabled:tw-opacity-70"
              type="button"
              disabled={isLocating}
              aria-busy={isLocating}
              onClick={handleFindNearMe}
            >
              <i className={`fas ${isLocating ? "fa-spinner fa-spin" : "fa-crosshairs"}`} aria-hidden="true" />
              {isLocating ? "Đang lấy vị trí..." : "Tìm bãi gần tôi"}
            </button>
          </div>
        </section>

        <section className="tw-mt-5 tw-grid tw-gap-4" aria-label="Thiết lập tìm kiếm">
          <div className="tw-flex tw-flex-wrap tw-items-center tw-justify-between tw-gap-3 tw-rounded-vm-lg tw-border tw-border-solid tw-border-slate-200 tw-bg-white tw-p-4 tw-shadow-sm">
            <div>
              <p className="tw-m-0 tw-text-xs tw-font-extrabold tw-uppercase tw-tracking-wide tw-text-slate-500">Bán kính tìm kiếm</p>
              <div className="tw-mt-2 tw-flex tw-gap-2" role="group" aria-label="Chọn bán kính tìm kiếm">
                {RADIUS_OPTIONS.map((option) => (
                  <button
                    className={`tw-min-h-10 tw-rounded-full tw-border tw-border-solid tw-px-4 tw-text-sm tw-font-black ${radiusKm === option ? "tw-border-blue-600 tw-bg-blue-600 tw-text-white" : "tw-border-slate-200 tw-bg-white tw-text-slate-700 hover:tw-bg-slate-50"}`}
                    key={option}
                    type="button"
                    aria-pressed={radiusKm === option}
                    onClick={() => handleRadiusChange(option)}
                  >
                    {option} km
                  </button>
                ))}
              </div>
            </div>
            <button
              className="tw-min-h-10 tw-rounded-full tw-border tw-border-solid tw-border-slate-300 tw-bg-white tw-px-4 tw-text-sm tw-font-extrabold tw-text-slate-800 hover:tw-bg-slate-50"
              type="button"
              aria-expanded={showFallback}
              onClick={() => setShowFallback((value) => !value)}
            >
              <i className="fas fa-map-pin tw-mr-2" aria-hidden="true" />
              Không dùng được GPS?
            </button>
          </div>

          {showFallback ? (
            <div className="tw-rounded-vm-lg tw-border tw-border-solid tw-border-slate-200 tw-bg-white tw-p-4 tw-shadow-sm sm:tw-p-6">
              <div className="tw-mb-4">
                <h2 className="tw-m-0 tw-text-lg tw-font-black tw-text-slate-900">Tìm bằng địa chỉ hoặc chọn trên bản đồ</h2>
                <p className="tw-mb-0 tw-mt-1 tw-text-sm tw-text-slate-600">Hỗ trợ cả địa chỉ hiện hành và địa chỉ cũ có cấp huyện.</p>
              </div>
              <ParkingAddressPicker value={address} onChange={setAddress} />
              <div className="tw-mt-4 tw-flex tw-flex-wrap tw-gap-2">
                <button
                  className="tw-min-h-11 tw-rounded-full tw-border-0 tw-bg-blue-600 tw-px-5 tw-font-extrabold tw-text-white hover:tw-bg-blue-700 disabled:tw-cursor-wait disabled:tw-opacity-60"
                  type="button"
                  disabled={isGeocoding || !featureStatus.geocodingProviderEnabled}
                  aria-busy={isGeocoding}
                  onClick={() => void handleAddressSearch()}
                >
                  <i className={`fas ${isGeocoding ? "fa-spinner fa-spin" : "fa-search-location"} tw-mr-2`} aria-hidden="true" />
                  {isGeocoding ? "Đang tìm tọa độ..." : featureStatus.geocodingProviderEnabled ? "Tìm tọa độ từ địa chỉ" : "Tra cứu địa chỉ đang tạm dừng"}
                </button>
                <button
                  className={`tw-min-h-11 tw-rounded-full tw-border tw-border-solid tw-px-5 tw-font-extrabold ${pickingEnabled ? "tw-border-amber-500 tw-bg-amber-100 tw-text-amber-950" : "tw-border-slate-300 tw-bg-white tw-text-slate-800"}`}
                  type="button"
                  aria-pressed={pickingEnabled}
                  onClick={() => {
                    setPickingEnabled((value) => !value);
                    setNotice(pickingEnabled ? null : "Hãy chạm hoặc nhấp vào vị trí mong muốn trên bản đồ.");
                  }}
                >
                  <i className="fas fa-map-marker-alt tw-mr-2" aria-hidden="true" />
                  {pickingEnabled ? "Đang chờ chọn điểm" : "Chọn điểm trên bản đồ"}
                </button>
              </div>
            </div>
          ) : null}
        </section>

        <div className="tw-mt-4" aria-live="polite" aria-atomic="true">
          {error ? (
            <div className="tw-rounded-vm-md tw-border tw-border-solid tw-border-red-200 tw-bg-red-50 tw-p-4 tw-font-bold tw-text-red-800" role="alert">
              <i className="fas fa-exclamation-circle tw-mr-2" aria-hidden="true" />{error}
            </div>
          ) : notice ? (
            <div className="tw-rounded-vm-md tw-border tw-border-solid tw-border-amber-200 tw-bg-amber-50 tw-p-4 tw-font-bold tw-text-amber-900">
              <i className="fas fa-info-circle tw-mr-2" aria-hidden="true" />{notice}
            </div>
          ) : null}
        </div>

        <section className="tw-mt-4 tw-grid tw-gap-4 lg:tw-grid-cols-[minmax(0,1.45fr)_minmax(340px,.75fr)]" aria-label="Kết quả tìm bãi xe">
          <div className="tw-relative tw-min-h-[360px] tw-overflow-hidden tw-rounded-vm-lg tw-border tw-border-solid tw-border-slate-200 tw-bg-white tw-shadow-sm lg:tw-sticky lg:tw-top-[88px] lg:tw-h-[calc(100vh-112px)] lg:tw-min-h-[540px]">
            <MapErrorBoundary onError={() => setMapFailed(true)}>
              <NearbyParkingMap
                lots={lots}
                onMapError={() => setMapFailed(true)}
                onPickLocation={handlePickLocation}
                onSelectLot={setSelectedLotId}
                pickingEnabled={pickingEnabled}
                selectedLotId={selectedLotId}
                userLocation={userLocation}
              />
            </MapErrorBoundary>
            {pickingEnabled ? (
              <div className="tw-pointer-events-none tw-absolute tw-left-3 tw-right-3 tw-top-3 tw-z-[500] tw-rounded-full tw-bg-amber-100/95 tw-px-4 tw-py-2 tw-text-center tw-text-sm tw-font-black tw-text-amber-950 tw-shadow">
                Chạm vào bản đồ để đặt vị trí tìm kiếm
              </div>
            ) : null}
          </div>

          <div className="tw-min-w-0">
            <div className="tw-mb-3 tw-flex tw-items-end tw-justify-between tw-gap-3">
              <div>
                <p className="tw-m-0 tw-text-xs tw-font-extrabold tw-uppercase tw-tracking-wide tw-text-blue-700">Kết quả gần nhất</p>
                <h2 className="tw-mb-0 tw-mt-1 tw-text-2xl tw-font-black tw-text-slate-950">Bãi xe trong {radiusKm} km</h2>
              </div>
              {userLocation && !isSearching ? <span className="tw-text-sm tw-font-bold tw-text-slate-500">{lots.length} bãi</span> : null}
            </div>

            {locationLabel ? (
              <p className="tw-rounded-vm-md tw-bg-blue-50 tw-p-3 tw-text-sm tw-font-bold tw-text-blue-900">
                <i className="fas fa-location-arrow tw-mr-2" aria-hidden="true" />{locationLabel}
              </p>
            ) : null}
            {mapFailed ? (
              <p className="tw-rounded-vm-md tw-border tw-border-solid tw-border-amber-200 tw-bg-amber-50 tw-p-3 tw-text-sm tw-font-bold tw-text-amber-900" role="status">
                Bản đồ hoặc tile đang lỗi. Danh sách và nút chỉ đường vẫn hoạt động bình thường.
              </p>
            ) : null}

            {isSearching ? (
              <div className="tw-grid tw-min-h-48 tw-place-items-center tw-rounded-vm-lg tw-border tw-border-solid tw-border-slate-200 tw-bg-white tw-p-8 tw-text-center" role="status">
                <div><i className="fas fa-spinner fa-spin tw-text-3xl tw-text-blue-600" aria-hidden="true" /><p className="tw-mb-0 tw-mt-3 tw-font-extrabold">Đang tìm bãi xe...</p></div>
              </div>
            ) : !userLocation ? (
              <div className="tw-rounded-vm-lg tw-border tw-border-dashed tw-border-slate-300 tw-bg-white tw-p-8 tw-text-center">
                <i className="fas fa-route tw-text-4xl tw-text-slate-300" aria-hidden="true" />
                <p className="tw-mb-1 tw-mt-4 tw-font-black tw-text-slate-900">Chưa có vị trí tìm kiếm</p>
                <p className="tw-m-0 tw-text-sm tw-text-slate-600">Bấm “Tìm bãi gần tôi” hoặc dùng lựa chọn thủ công.</p>
              </div>
            ) : !error && lots.length === 0 ? (
              <div className="tw-rounded-vm-lg tw-border tw-border-solid tw-border-slate-200 tw-bg-white tw-p-8 tw-text-center" role="status">
                <i className="fas fa-parking tw-text-4xl tw-text-slate-300" aria-hidden="true" />
                <p className="tw-mb-1 tw-mt-4 tw-font-black tw-text-slate-900">Không có bãi trong {radiusKm} km</p>
                <p className="tw-m-0 tw-text-sm tw-text-slate-600">Hãy thử tăng bán kính hoặc chọn một điểm khác.</p>
              </div>
            ) : (
              <ol className="tw-m-0 tw-grid tw-list-none tw-gap-3 tw-p-0" aria-label="Danh sách bãi xe theo khoảng cách tăng dần">
                {lots.map((lot, index) => {
                  const selected = lot.parkingLotId === selectedLotId;
                  return (
                    <li key={lot.parkingLotId}>
                      <article className={`tw-rounded-vm-lg tw-border tw-border-solid tw-bg-white tw-p-4 tw-shadow-sm tw-transition ${selected ? "tw-border-blue-500 tw-ring-2 tw-ring-blue-100" : "tw-border-slate-200"}`}>
                        <button
                          className="tw-w-full tw-border-0 tw-bg-transparent tw-p-0 tw-text-left"
                          type="button"
                          aria-expanded={selected}
                          onClick={() => setSelectedLotId(lot.parkingLotId)}
                        >
                          <div className="tw-flex tw-items-start tw-gap-3">
                            <span className="tw-grid tw-h-9 tw-w-9 tw-flex-none tw-place-items-center tw-rounded-full tw-bg-teal-700 tw-font-black tw-text-white" aria-hidden="true">{index + 1}</span>
                            <span className="tw-min-w-0 tw-flex-1">
                              <strong className="tw-block tw-text-base tw-font-black tw-text-slate-950">{lot.name}</strong>
                              <span className="tw-mt-1 tw-block tw-text-sm tw-leading-5 tw-text-slate-600">{lot.addressDisplay || "Địa chỉ đang cập nhật"}</span>
                            </span>
                            <span className="tw-whitespace-nowrap tw-rounded-full tw-bg-blue-50 tw-px-3 tw-py-1 tw-text-sm tw-font-black tw-text-blue-800">{formatDistance(lot)}</span>
                          </div>
                        </button>
                        {selected && userLocation ? (
                          <div className="tw-mt-4 tw-border-0 tw-border-t tw-border-solid tw-border-slate-100 tw-pt-4">
                            <a
                              className="tw-inline-flex tw-min-h-11 tw-w-full tw-items-center tw-justify-center tw-gap-2 tw-rounded-full tw-bg-slate-950 tw-px-4 tw-font-black tw-text-white hover:tw-bg-blue-700 hover:tw-text-white hover:tw-no-underline"
                              href={createDirectionsUrl(userLocation, lot)}
                              target="_blank"
                              rel="noopener noreferrer"
                              aria-label={`Mở Google Maps chỉ đường đến ${lot.name}`}
                            >
                              <i className="fas fa-directions" aria-hidden="true" /> Chỉ đường bằng Google Maps
                            </a>
                          </div>
                        ) : null}
                      </article>
                    </li>
                  );
                })}
              </ol>
            )}

            {selectedLot && userLocation ? (
              <p className="tw-mb-0 tw-mt-4 tw-text-center tw-text-xs tw-leading-5 tw-text-slate-500">
                Google Maps sẽ mở ở tab mới với chế độ lái xe. Liên kết chỉ đường không cần API key.
              </p>
            ) : null}
          </div>
        </section>
      </div>
    </main>
  );
}
