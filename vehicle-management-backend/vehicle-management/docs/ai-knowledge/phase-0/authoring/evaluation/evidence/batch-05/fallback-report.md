# Kiểm chứng key trước, model sau

Ngày 2026-11-27. Bộ giả lập HTTP và policy hiện có đã qua 49 test, không gọi Google thật. Xem [báo cáo JUnit](java-tests.json). Không dùng kết quả này làm nghiệm thu câu trả lời Google.

| Hành vi | Bằng chứng test |
|---|---|
| Key chính/model A dùng được, không gọi key sau | `GeminiCredentialPoolTest.primarySuccessNeverTouchesBackup` |
| Hết quota A, dùng B cùng key, ghi nhớ lựa chọn | `exhaustsModelsOnCurrentKeyBeforeChangingCredentialAndRemembersModel` |
| Chỉ sang key sau khi không còn model hợp lệ ở key trước | Test trên cùng pool; kiểm cả thứ tự HTTP và sticky selection |
| Quota phút/500/503 dùng cooldown và Retry-After/RetryInfo | `temporaryFailureCanUseNextModelWithoutChangingKey`, `retryAfterHttpDateAndDailyResetUseProviderTime` |
| Key cùng project không là quota độc lập | `dailyQuotaSkipsSiblingKeysInSameProjectButNotAnotherProject`, `GeminiCredentialAvailabilityTest` |
| Auth cách ly key; permission model không vô hiệu cả key | `invalidKeyIsNotRetriedForEveryModelAndBackupStaysSelected`, `modelPermissionDoesNotInvalidateKeyForAnotherModel` |
| Request sai/safety không quét model/key | `badRequestDoesNotScanOtherModelsOrKeys`, `safetyBlockNeverSwitchesModel` |
| Embedding đổi credential nhưng giữ model/dimension | `embeddingChangesCredentialWithoutChangingModelOrDimension` |
| Deadline không có nghĩa mọi key đã hết quota | `maximumAttemptsCapsWireCallsAndDoesNotMeanAllKeysWereTested`, `expiredPlanCannotMakeAnHttpRequest`, `shortRemainingBudgetDoesNotFalselyDisableTheNextModel` |
| Bí mật được loại khỏi diagnostics/response | `rawProviderEchoOfAnyConfiguredKeyIsRedactedBeforeMapping`, test serialize diagnostic |

Registry/policy test kiểm ACTIVE/FALLBACK, rollout và free-tier approval; không tự thêm model qua catalog. Không có thay đổi hành vi fallback/provider trong đợt 5 vì các yêu cầu nêu trên đã được cơ chế hiện tại kiểm chứng.

Thứ tự Google live thật nằm trong `wire-attempts.json` theo từng thư mục cohort và [tổng hợp cohort](cohort-summary.json). Phân biệt catalog, embedding và generation; số HTTP chỉ đếm request thật proxy chuyển đến Google. Không suy từ số case, diagnostics cuối hoặc kết quả giả lập.

Mỗi attempt ghi alias biến credential, model trong đường gọi, thời điểm, HTTP status, Retry-After và độ trễ. `credential-failures.json` giữ loại lỗi; credential chưa gửi request phải giữ UNTESTED. Một request hết deadline không được báo là hết quota toàn bộ. Không suy số quota còn lại khi Google không cung cấp.
