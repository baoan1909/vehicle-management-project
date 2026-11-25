package com.ban.vehicle_management.domain.ai.model;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Backend-owned intent routing. Matching views never replace identifiers or authorize actions. */
public final class AssistantIntentRouter {
    private static final Pattern SECURITY = pattern(
            "(?:bo qua|vuot qua).{0,25}(?:quyen|xac nhan)",
            "khong can.{0,15}(?:hoi lai|xac nhan)",
            "(?:toi|minh|em).{0,15}gui.{0,40}(?:cccd|cmnd|can cuoc|giay to dinh danh).{0,20}(?:day du|nguyen)",
            "(?:dua|tra|lay|xem).{0,50}(?:cua nguoi khac|khach hang ben canh)",
            "(?:xuat|lay|gui|xem).{0,30}(?:toan bo|moi).{0,30}(?:anh camera|phien|luot gui)",
            "(?:system\\s+prompt|ignore\\s+(?:previous|all)\\s+instructions|bo qua.{0,30}(?:huong dan|chi dan))",
            "(?:tiet lo|cho.{0,15}xem|hien thi|doc|gui|lay).{0,40}(?:api[-_ ]?key|secret|access token|refresh token|mat khau.{0,15}(?:nguoi khac|he thong))",
            "(?:xem|doc|lay|tra cuu).{0,50}(?:cua nguoi khac|tai khoan khac|khach hang khac)",
            "(?:cho (?:toi|minh|em)|xem|doc|lay|tra cuu).{0,60}(?:khach khac|khach hang khac|nguoi khac|tenant khac)",
            "(?:bo qua|bo moi).{0,30}(?:xac nhan|kiem tra oauth|quy tac)",
            "(?:token|api[-_ ]?key|mat khau).{0,30}(?:hay luu|luu lai)",
            "(?:api[-_ ]?key|secret|token|password|mat khau|otp|cvv|cvc)\\s*[:=]\\s*\\S+",
            "(?:cvv|cvc)\\s*\\d{3,4}",
            "(?:toi|minh|em)\\s+(?:se\\s+)?gui.{0,30}(?:otp|token|mat khau|so the|ma bao mat)");
    private static final Pattern GREETING = Pattern.compile(
            "^(?:xin chao|chao(?: ban)?|hello|hi|hey|chao buoi (?:sang|chieu|toi))(?: coparking)?[!.,\\s]*$");
    private static final Pattern HANDOFF = pattern(
            "(?:gap|noi chuyen|ket noi|chuyen).{0,25}(?:nhan vien|nguoi that|tong dai|ho tro vien)",
            "doi nhan vien ho tro|muon quan ly xem xet|quan ly phai phan cong");
    private static final Pattern OUTSIDE = pattern("chung khoan|forex|crypto|bitcoin|bong da|du doan xo so");
    private static final Pattern GUIDE = pattern(
            "bao nhieu|sau khi|truoc khi",
            "nhu the nao|nhu nao|the nao|ra sao|lam sao|lam the nao|can lam gi|phai lam sao|huong dan|quy trinh|quy dinh|dieu kien|thu tuc|cach|o dau|tai sao|vi sao|la gi|nghia la|co nen|bao lau|duoc khong|co\\b.{0,60}\\bkhong|co\\b.{0,50}\\bduoc khong|can\\b.{0,25}\\b(?:gi|nao)");
    private static final Pattern WRITE = pattern(
            "(?:mo|dong|nang|ha).{0,15}(?:cong|barrier|rao chan)(?:.{0,25}(?:giup|ho|dum|cho)\\s+(?:toi|minh|em)|$)",
            "(?:ghi nhan|xac nhan|check in|check out|check-in|check-out).{0,20}(?:xe vao|xe ra|phien gui|giup toi)",
            "(?:thanh toan|hoan tien|ap dung ma|ap ma|chuyen tien).{0,50}(?:giup|ho|dum|cho)\\s+(?:toi|minh|em)",
            "(?:sua|xoa|danh dau).{0,20}(?:hoa don|giao dich|so tien)",
            "(?:thanh toan|hoan tien|ap dung ma|ap ma|dat cho|giu cho)(?:\\s+(?:giup|cho|ho|dum|toi|minh|em)){1,4}",
            "(?:huy|xac nhan).{0,20}(?:dat cho|giu cho|hoa don|giao dich)",
            "(?:tao|mo|lap)(?:\\s+(?:giup|cho|ho|dum|toi|minh|em|mot|1|cai)){0,6}\\s+(?:phieu|ticket|yeu cau)",
            "(?:them|sua|xoa|ngung (?:su )?dung|kich hoat|dat(?!\\s+cho)).{0,25}(?:xe|phuong tien)",
            "(?:dang ky|gia han|huy|khoa|mo khoa).{0,25}(?:ve|subscription)",
            "(?:bao mat|khoa|mo khoa|cap lai|thay).{0,20}the",
            "(?:sua|xoa|doi).{0,20}(?:lich su|gio vao|gio ra)");
    private static final Pattern NEGATED_WRITE = pattern(
            "(?:dung|khong(?: muon| can)?|chua(?: muon)?)\\s+(?:tu\\s+)?(?:tao|mo|lap|them|sua|xoa|gia han|huy|khoa|bao mat)");
    private static final Pattern FAILED_OPERATION = pattern(
            "(?:loi|khong biet|khong the|chua biet).{0,20}(?:tao|mo|lap).{0,20}(?:phieu|ticket)",
            "(?:tao|mo|lap).{0,25}(?:phieu|ticket).{0,20}(?:bi loi|khong duoc|khong thanh cong|that bai)");
    private static final Pattern PERSONAL = pattern(
            "(?:hoa don|giao dich|dat cho|khoan thanh toan).{0,35}(?:cua toi|cua minh|cua em)",
            "(?:cua toi|cua minh|cua em|my).{0,40}(?:phieu|ve|dang ky|ticket|subscription|xe|phuong tien|ho so|the|lich su)",
            "(?:phieu|ve|dang ky|ticket|subscription|xe|phuong tien|ho so|the|lich su).{0,35}(?:cua toi|cua minh|cua em)",
            "(?:xe|ve|the)\\s+(?:toi|minh|em).{0,30}(?:da ra|con han|het han|dang gui|bi khoa)",
            "(?:xem|tra cuu|kiem tra|liet ke).{0,20}(?:lich su gui xe|xe da dang ky|ve dang su dung)");
    private static final Pattern PROCEDURAL = pattern("huong dan|quy trinh|quy dinh|thu tuc|cach|o dau|lam sao|lam the nao|nhu the nao|nhu nao|the nao|ra sao|duoc khong|co\\b.{0,60}\\bkhong");
    private static final Pattern OPERATION_ERROR = pattern("^loi|bi loi|khong duoc|chua duoc|khong thanh cong|that bai|khong the|bi thieu|chua cap nhat");
    private static final Pattern STRONG_GUIDE = pattern("huong dan|quy trinh|quy dinh|thu tuc|cach|lam sao|lam the nao|nhu the nao|nhu nao|phai lam sao|can lam gi");
    private static final Pattern OWNER = pattern("cua toi|cua minh|cua em|my|(?:xe|ve|the) (?:toi|minh|em)");
    private static final Pattern OWN_RECENT_HISTORY = pattern("(?:cho (?:toi|minh|em) xem).{0,30}(?:lan gui xe|luot gui xe)");
    private static final Pattern WHY = pattern("tai sao|vi sao");
    private static final Pattern EXPLICIT_READ = pattern("xem|tra cuu|kiem tra|liet ke");
    private static final Pattern IDENTIFIER = Pattern.compile("\\b[A-Z]{2,5}-\\d{4,}\\b");
    private static final Pattern TOPIC = pattern(
            "vao bai|ra bai|xe vang lai|the vang lai|the (?:da )?co (?:xe|luot)|luot chua ket thuc|bai nao|bai con cho|bai (?:con |dang )?hoat dong|trang thai bai|tim quanh vi tri|tru so|dia chi lien he|loi ket noi|tro ly|tru tien",
            "tim bai|bai gan|ban do|dinh vi|gps|chi duong|toa do|ban kinh|gio mo cua|gio dong cua|hotline|phan hoi|cong bai xe|cong xe|cong (?:khong|chua|tu choi)|loi cong|mo cong|qua(?: duoc)? cong|dong cong|quet the|quet qr|nhan dang bien so|phien gui xe|failed to fetch",
            "hoa don|voucher|khuyen mai|uu dai|ma giam|dat cho|giu cho|dat truoc|phi qua dem|tinh phi|tinh tien|chuyen khoan|phi ban ngay|phi ban dem|gia hien tai|muc phi|phi gui|ranh gioi gio ban ngay|tra tien|ve chua hieu luc",
            "dang nhap|dang ky|mat khau|xac minh|email|google|oauth|ho so|phieu|ticket|ho tro|hoi thoai|chat|quyen|bao mat|du lieu|token|otp|api[-_ ]?key|giay to dinh danh|ve thang|subscription|bang gia|phi gui xe|thanh toan|dong tien|huy ve|vnpay|sepay|hoan tien|xe vao|xe ra|the xe|mat the|gio hoat dong|dia diem|sla|so do|bai xe|he thong|doi tac|don hang|chatbot|noi dung loi|bao loi|lien he|phuong tien|bien so|xe mac dinh|them xe|sua xe|xoa xe|ngung dung xe|lich su|luot gui|lan gui xe|gio vao|gio ra|gia han|cap lai the|khoa the|the bi khoa|tim thay the|tim lai the|ve dang|ve da|ve con|ve cho|ve bat dau|xe da ngung dung|xe bi khoa|trang thai phien");
    private static final Pattern VAGUE = Pattern.compile(
            "^(?:loi|van loi|bao loi|khong duoc|khong duoc nua|van khong duoc|khong biet|lam sao|nhu nao|nhu the nao|phai lam sao|can lam gi|buoc tiep theo|con buoc sau|cai do o dau|toi lam roi nhung chua duoc|ve thang|phieu|ticket|ho so|thanh toan|the|ve|xe|phuong tien|bien so|lich su|mat the)[?.!\\s]*$");

    private AssistantIntentRouter() { }

    public record IntentDecision(AssistantIntent intent, String reason) { }

    public static IntentDecision route(String rawInput) {
        if (rawInput == null || rawInput.isBlank()) return decision(AssistantIntent.OUT_OF_SCOPE, "empty-input");
        String text;
        try {
            text = matching(rawInput);
        } catch (IllegalArgumentException exception) {
            return decision(AssistantIntent.CLARIFICATION, "input-too-long");
        }
        if (SECURITY.matcher(text).find()) return decision(AssistantIntent.SECURITY_REFUSAL, "deterministic-sensitive");
        if (GREETING.matcher(text).matches()) return decision(AssistantIntent.GREETING, "deterministic-greeting");
        if (HANDOFF.matcher(text).find()) return decision(AssistantIntent.HANDOFF_REQUEST, "deterministic-handoff");
        if (OUTSIDE.matcher(text).find()) return decision(AssistantIntent.OUT_OF_SCOPE, "deterministic-out-of-scope");
        if (VAGUE.matcher(text).matches()) return decision(AssistantIntent.CLARIFICATION, "missing-topic-or-goal");
        boolean guide = GUIDE.matcher(text).find() || NEGATED_WRITE.matcher(text).find()
                || FAILED_OPERATION.matcher(text).find() || OPERATION_ERROR.matcher(text).find();
        if (!STRONG_GUIDE.matcher(text).find() && !OPERATION_ERROR.matcher(text).find() && !WHY.matcher(text).find()
                && (!guide || OWNER.matcher(text).find()) && EXPLICIT_READ.matcher(text).find() && PERSONAL.matcher(text).find()) return decision(AssistantIntent.PERSONAL_DATA, "explicit-personal-read");
        if (WRITE.matcher(text).find() && !guide) return decision(AssistantIntent.WRITE_ACTION, "explicit-write-request");
        boolean procedural = PROCEDURAL.matcher(text).find()
                || FAILED_OPERATION.matcher(text).find() || OPERATION_ERROR.matcher(text).find()
                || WHY.matcher(text).find();
        boolean personalQuestion = OWNER.matcher(text).find()
                && !STRONG_GUIDE.matcher(text).find() && !OPERATION_ERROR.matcher(text).find() && !WHY.matcher(text).find();
        if ((!procedural || personalQuestion) && (PERSONAL.matcher(text).find() || OWN_RECENT_HISTORY.matcher(text).find() || IDENTIFIER.matcher(rawInput).find())) {
            return decision(AssistantIntent.PERSONAL_DATA, "deterministic-personal");
        }
        if (TOPIC.matcher(text).find()) return decision(AssistantIntent.STATIC_KNOWLEDGE, "deterministic-domain-knowledge");
        if (guide || text.contains("?")) return decision(AssistantIntent.CLARIFICATION, "question-without-domain");
        return decision(AssistantIntent.OUT_OF_SCOPE, "fallback-out-of-scope");
    }

    /** Limited conversational aliases; never passed to repositories as corrected entity values. */
    public static String matching(String input) {
        return VietnameseQueryNormalizer.normalize(input).folded()
                .replaceAll("\\bdk\\b", "dang ky")
                .replaceAll("\\b(?:ko|khongg)\\b", "khong")
                .replaceAll("\\bdc\\b", "duoc");
    }

    public static IntentDecision fromModelClassifier(String value) {
        if (value != null) {
            try {
                return decision(AssistantIntent.valueOf(value.strip().toUpperCase(Locale.ROOT)), "model-classified");
            } catch (IllegalArgumentException ignored) { }
        }
        return decision(AssistantIntent.OUT_OF_SCOPE, "model-unknown");
    }

    private static IntentDecision decision(AssistantIntent intent, String reason) {
        return new IntentDecision(intent, reason);
    }

    private static Pattern pattern(String... alternatives) {
        return Pattern.compile("\\b(?:" + String.join("|", List.of(alternatives)) + ")\\b");
    }
}
