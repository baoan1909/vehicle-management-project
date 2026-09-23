INSERT INTO ai.knowledge_sources (source_id, title, access_scope, status)
VALUES ('7f4f11d7-2b52-4c20-bf11-0e4200000601', 'CoParking support handbook', 'PUBLIC', 'ACTIVE')
ON CONFLICT (source_id) DO NOTHING;

INSERT INTO ai.knowledge_documents (
    document_id, source_id, title, document_version, access_scope, status
) VALUES (
    '7f4f11d7-2b52-4c20-bf11-0e4200000602',
    '7f4f11d7-2b52-4c20-bf11-0e4200000601',
    'Huong dan ho tro khach hang CoParking',
    1,
    'PUBLIC',
    'READY'
) ON CONFLICT (document_id) DO NOTHING;

INSERT INTO ai.knowledge_chunks (
    chunk_id, document_id, title, content, summary, source_section, chunk_index, access_scope, document_version, status
) VALUES
    (
        '7f4f11d7-2b52-4c20-bf11-0e4200000611',
        '7f4f11d7-2b52-4c20-bf11-0e4200000602',
        'Dang ky ve thang',
        'Khach hang dang ky ve thang trong cong thong tin, chon phuong tien, loai ve va ngay hieu luc mong muon. Yeu cau se duoc nhan vien duyet, tao hoa don va gan the sau khi thanh toan.',
        'Quy trinh dang ky ve thang gom gui yeu cau, duyet, thanh toan va gan the.',
        'Dang ky ve',
        1,
        'PUBLIC',
        1,
        'READY'
    ),
    (
        '7f4f11d7-2b52-4c20-bf11-0e4200000612',
        '7f4f11d7-2b52-4c20-bf11-0e4200000602',
        'Thanh toan hoa don',
        'Hoa don dang ky ve co the duoc thanh toan qua cong thanh toan duoc cau hinh. Sau khi thanh toan thanh cong, he thong cap nhat hoa don va kich hoat buoc gan the.',
        'Thanh toan thanh cong la dieu kien de tiep tuc gan the cho ve.',
        'Thanh toan',
        2,
        'PUBLIC',
        1,
        'READY'
    ),
    (
        '7f4f11d7-2b52-4c20-bf11-0e4200000613',
        '7f4f11d7-2b52-4c20-bf11-0e4200000602',
        'Xu ly su co',
        'Khi gap loi vao ra, mat the, hoa don hoac can nhan vien ho tro, khach hang nen tao phieu ho tro kem mo ta ngan gon va bang chung neu co.',
        'Su co can duoc ghi nhan bang phieu ho tro.',
        'Su co',
        3,
        'PUBLIC',
        1,
        'READY'
    )
ON CONFLICT (chunk_id) DO NOTHING;
