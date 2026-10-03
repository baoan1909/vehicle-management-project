import http from "k6/http";
import { check } from "k6";

http.setResponseCallback(http.expectedStatuses({ min: 200, max: 399 }, 429));

const baseUrl = __ENV.BASE_URL || "http://127.0.0.1:8080/vehicle-management";

export const options = {
  scenarios: {
    nearby_search: {
      executor: "constant-arrival-rate",
      rate: Number(__ENV.REQUESTS_PER_SECOND || 20),
      timeUnit: "1s",
      duration: __ENV.DURATION || "2m",
      preAllocatedVUs: 20,
      maxVUs: 100,
    },
  },
  thresholds: {
    http_req_failed: ["rate<0.01"],
    http_req_duration: ["p(95)<250", "p(99)<500"],
  },
};

export default function () {
  const latitude = 10.7769 + (Math.random() - 0.5) * 0.01;
  const longitude = 106.7009 + (Math.random() - 0.5) * 0.01;
  const response = http.get(
    `${baseUrl}/api/public/parking-lots/nearby?latitude=${latitude}&longitude=${longitude}&radiusKm=5&limit=20`,
    { tags: { endpoint: "nearby" } },
  );
  check(response, {
    "nearby status is 200 or rate limited": (result) => result.status === 200 || result.status === 429,
    "response does not expose internal organization": (result) => !result.body.includes("organizationId"),
  });
}
