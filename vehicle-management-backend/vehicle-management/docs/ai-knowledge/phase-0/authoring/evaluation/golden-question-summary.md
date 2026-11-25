# Tổng hợp bộ câu hỏi vàng

| Thuộc tính | Giá trị |
|---|---|
| Phiên bản | 1.0.0 |
| Trạng thái | DRAFT |
| Tổng số | 120 |
| Miền | 12 |
| Phân bổ | 10 case/miền |

## Phân bổ miền

| Prefix | Domain | Số case | Knowledge slug |
|---|---|---:|---|
| AUTH | AUTHENTICATION | 10 | authentication-and-profile |
| VEH | VEHICLE | 10 | customer-vehicle-management |
| SUB | SUBSCRIPTION | 10 | subscription-lifecycle |
| PRICE | PRICING | 10 | pricing-and-fee-calculation |
| BILL | BILLING | 10 | invoice-vnpay-payment-refund |
| PARK | PARKING | 10 | parking-entry-exit |
| CARD | CARD | 10 | cards-and-lost-card |
| HIST | PARKING_HISTORY | 10 | parking-history |
| SUP | SUPPORT | 10 | support-ticket-and-escalation |
| PRIV | PRIVACY | 10 | chatbot-privacy-and-prohibited-data |
| ERR | TROUBLESHOOTING | 10 | common-errors-and-troubleshooting |
| LOC | LOCATION | 10 | locations-hours-sla-contact |

## Coverage

Bộ dữ liệu có câu có dấu, không dấu, typo, câu ngắn, multi-turn, ambiguous, personal runtime, write/action confirmation, no-source, out-of-scope, prompt injection, cross-account, secret, payment và complaint/escalation. Không chứa dữ liệu cá nhân thật.

## Cách tạo lại

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File docs/ai-knowledge/phase-0/authoring/evaluation/generate-golden-questions.ps1
```

## Cách kiểm tra

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File docs/ai-knowledge/phase-0/authoring/evaluation/validate-phase0.ps1
```

