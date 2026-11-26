-- 봉쇄구역 회수반 사건 1의 기본 데이터
SET DEFINE OFF
SET SERVEROUTPUT ON
SET AUTOCOMMIT OFF
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM CASE_FILE WHERE case_id=1;
    IF v_count <> 0 THEN RAISE_APPLICATION_ERROR(-20004, '시연 사건 1이 이미 있습니다. 중복 입력하지 않습니다.'); END IF;
END;
/

-- CASE_FILE 사건 제목과 브리핑
INSERT INTO CASE_FILE (case_id, title, briefing)
VALUES (1, '봉쇄구역 회수반: 닫힌 문을 연 기록', '야간 회수반에 보안실 CCTV를 확보하라는 지시가 내려왔다. 대피 명단에는 모두 빠져나갔다고 적혀 있지만, 현장 기록은 다른 이야기를 하고 있다. 영상과 출입 기록을 확인한 뒤 탈출 경로를 선택하자.');

-- AREA 시설 구역과 시작·탈출 지점
INSERT INTO AREA (case_id, area_no, area_name, area_type, map_x, map_y)
VALUES (1, 1, '옥상 진입점', 'START', 0, 0);

INSERT INTO AREA (case_id, area_no, area_name, area_type, map_x, map_y)
VALUES (1, 2, '옥상 탈출점', 'EXIT', 0, 1);

INSERT INTO AREA (case_id, area_no, area_name, area_type, map_x, map_y)
VALUES (1, 3, '중앙 복도', 'NORMAL', 1, 0);

INSERT INTO AREA (case_id, area_no, area_name, area_type, map_x, map_y)
VALUES (1, 4, '보안실', 'NORMAL', 2, 0);

INSERT INTO AREA (case_id, area_no, area_name, area_type, map_x, map_y)
VALUES (1, 5, '발전 제어실', 'NORMAL', 1, 1);

INSERT INTO AREA (case_id, area_no, area_name, area_type, map_x, map_y)
VALUES (1, 6, '하역장', 'EXIT', 2, 1);

-- INSPECTION 구역별 단서, 대화, 조사 기록
INSERT INTO INSPECTION (case_id, inspection_no, area_no, inspection_type, title, content, display_order)
VALUES (1, 1, 1, 'DIALOGUE', '회수 지시', '무전 담당자: 회수반, 들리나? 봉쇄가 풀리기 전에 보안실 CCTV 기록부터 확보해.' || CHR(10) || '회수반원: 대피 명단에 있는 사람들은 다 빠져나갔습니까?' || CHR(10) || '무전 담당자: 명단에는 전원 대피라고 돼 있어. 우선 영상만 복사해서 돌아와.', 1);

INSERT INTO INSPECTION (case_id, inspection_no, area_no, inspection_type, title, content, display_order)
VALUES (1, 2, 3, 'CLUE', '뒤섞인 상황판', '상황판 표시 기록 / 봉쇄 21:17 / 이상 징후 21:10 / 경보 21:12. 실제 발생 순서: 이상 징후 → 경보 → 봉쇄.', 1);

INSERT INTO INSPECTION (case_id, inspection_no, area_no, inspection_type, title, content, display_order)
VALUES (1, 3, 4, 'CLUE', '보안 잠금 메모', '잠금 번호는 상황판 기록을 발생 순서대로 읽어 만든다. 각 표시 시각의 분 일의 자리만 이어 붙인다. 첫 숫자가 0이어도 그대로 입력한다.', 1);

INSERT INTO INSPECTION (case_id, inspection_no, area_no, inspection_type, title, content, display_order)
VALUES (1, 4, 4, 'STORY', '대피되지 않은 이름', '대피 명단 맨 끝에 있던 세 이름이 줄로 지워져 있다. 명단 아래에는 ‘전원 대피’ 확인 도장이 찍혀 있다. 도장 옆의 메모는 짧다. ‘출구가 먼저 잠겼다.’', 2);

INSERT INTO INSPECTION (case_id, inspection_no, area_no, inspection_type, title, content, display_order)
VALUES (1, 5, 4, 'CLUE', 'CCTV 시계 점검표', '점검 결과: CCTV 시계는 실제 시각보다 2분 빠르다. 출입문이 잠기는 화면에 표시된 시각은 21:17이다.', 3);

INSERT INTO INSPECTION (case_id, inspection_no, area_no, inspection_type, title, content, display_order)
VALUES (1, 6, 4, 'CLUE', '출입 명령 목록', '21:12 A13 경보 발령 / 21:13 D07 출입 차단 / 21:15 B42 출입 차단 / 21:16 C09 봉쇄 승인 / 21:17 E26 출입 차단. 차단 명령 중 실제 시각에 일치하는 코드를 찾는다.', 4);

INSERT INTO INSPECTION (case_id, inspection_no, area_no, inspection_type, title, content, display_order)
VALUES (1, 7, 5, 'CLUE', '비상 전력 기준', '비상 전력 한도는 6이다. 필수 장비: 화물 승강기, 복도 조명. 실험실 배기는 필요하지 않다.', 1);

INSERT INTO INSPECTION (case_id, inspection_no, area_no, inspection_type, title, content, display_order)
VALUES (1, 8, 5, 'CLUE', '세 회로의 부하', '회로별 전력 소모: 승강기 4, 조명 2, 배기 3. 입력 자리 순서: 승강기 → 조명 → 배기. 켜짐은 1, 꺼짐은 0으로 표시한다. 스위치를 조작하려면 양손이 비어 있어야 한다.', 2);

INSERT INTO INSPECTION (case_id, inspection_no, area_no, inspection_type, title, content, display_order)
VALUES (1, 9, 5, 'DIALOGUE', '남겨진 음성', '작업자 녹음: 아직 안에 사람이 있어. 승인도 안 났는데 문부터 잠가 버렸어.' || CHR(10) || '회수반원: 명단에는 다 나갔다고 했는데. CCTV 시각과 출입 기록을 맞춰 봐야겠어.', 3);

INSERT INTO INSPECTION (case_id, inspection_no, area_no, inspection_type, title, content, display_order)
VALUES (1, 10, 3, 'STORY', '원본 보존 주의', '영상 복사본에는 화면만 담긴다. 시계 보정 이력과 원본 서명은 녹화 장치에 남는다. 장치를 옮기려면 양손을 써야 하므로 사다리 대신 화물 승강기를 이용한다.', 2);

INSERT INTO INSPECTION (case_id, inspection_no, area_no, inspection_type, title, content, display_order)
VALUES (1, 11, 2, 'DIALOGUE', '옥상에서 온 독촉', '무전 담당자: 사다리 아래에서 대기 중이야. 장비를 들고는 못 내려오니 영상만 챙겨 와.' || CHR(10) || '회수반원: 영상만으로는 문이 잠긴 정확한 시각을 입증하기 어렵습니다.' || CHR(10) || '무전 담당자: 원본을 가져갈 거면 화물 승강기로 가. 하역장에서도 나갈 수 있어.', 1);

INSERT INTO INSPECTION (case_id, inspection_no, area_no, inspection_type, title, content, display_order)
VALUES (1, 12, 6, 'DIALOGUE', '하역장 마지막 통신', '무전 담당자: 승강기 전원이 돌아왔어. 지금이면 하역장으로 나올 수 있어.' || CHR(10) || '회수반원: 출입 기록을 대조했습니다. 문이 잠긴 시각이 공식 승인보다 앞섭니다.' || CHR(10) || '무전 담당자: 자세한 얘기는 밖에서 듣자. 경사로 쪽으로 나와.', 1);

-- PUZZLE 퍼즐의 정답, 실마리, 선행 조건
INSERT INTO PUZZLE (case_id, puzzle_no, area_no, puzzle_name, puzzle_type, prompt, answer_key, hint_text, solved_text, prerequisite_puzzle_no, required_free_hands)
VALUES (1, 1, 4, '뒤섞인 기록', 'CODE', '상황판을 발생 순서대로 읽어 잠금 번호 세 자리를 입력하자.', '027', '상황판에 적힌 발생 순서는 이상 징후(21:10), 경보(21:12), 봉쇄(21:17)다.', '기록함이 열렸다. CCTV 녹화 장치를 원본으로 가져가거나 영상만 복사할 수 있다.', NULL, 0);

INSERT INTO PUZZLE (case_id, puzzle_no, area_no, puzzle_name, puzzle_type, prompt, answer_key, hint_text, solved_text, prerequisite_puzzle_no, required_free_hands)
VALUES (1, 2, 5, '남은 전력 6', 'POWER', '비상 전력 한도 안에서 필요한 회로만 켜자. 승강기, 조명, 배기 순서로 설정한다.', '110', '화물 승강기(4)와 복도 조명(2)을 켜면 전력 한도 6에 맞는다. 실험실 배기(3)까지 켜면 한도를 넘는다.', '복도 조명이 켜지고 화물 승강기에 전원이 들어왔다. 통로 잠금을 풀려면 보안실에서 CCTV 시각과 출입 명령을 대조해야 한다.', 1, 2);

INSERT INTO PUZZLE (case_id, puzzle_no, area_no, puzzle_name, puzzle_type, prompt, answer_key, hint_text, solved_text, prerequisite_puzzle_no, required_free_hands)
VALUES (1, 3, 4, '승인보다 빠른 문', 'VERIFY', 'CCTV 시각을 실제 시각으로 바꾼 뒤, 그때의 출입 차단 명령 코드를 입력하자.', 'B42', 'CCTV에 표시된 21:17에서 시계 오차 2분을 빼면 실제 시각은 21:15다. 목록에서 그 시각에 실행된 출입 차단 명령을 찾는다.', '문이 잠긴 시각은 21:15, 봉쇄가 승인된 시각은 21:16이었다. 출입 로그를 복사할 수 있게 됐고, 두 탈출 경로의 잠금도 풀렸다.', 2, 0);

-- PUZZLE_CLUE 퍼즐과 조사 장면의 연결
INSERT INTO PUZZLE_CLUE (case_id, puzzle_no, inspection_no)
VALUES (1, 1, 2);

INSERT INTO PUZZLE_CLUE (case_id, puzzle_no, inspection_no)
VALUES (1, 1, 3);

INSERT INTO PUZZLE_CLUE (case_id, puzzle_no, inspection_no)
VALUES (1, 2, 7);

INSERT INTO PUZZLE_CLUE (case_id, puzzle_no, inspection_no)
VALUES (1, 2, 8);

INSERT INTO PUZZLE_CLUE (case_id, puzzle_no, inspection_no)
VALUES (1, 3, 5);

INSERT INTO PUZZLE_CLUE (case_id, puzzle_no, inspection_no)
VALUES (1, 3, 6);

-- PASSAGE 구역 사이의 이동 경로와 통과 조건
INSERT INTO PASSAGE (case_id, passage_no, passage_name, from_area_no, to_area_no, min_free_hands, requires_power, unlock_puzzle_no)
VALUES (1, 1, '진입 사다리', 1, 3, 2, 'N', NULL);

INSERT INTO PASSAGE (case_id, passage_no, passage_name, from_area_no, to_area_no, min_free_hands, requires_power, unlock_puzzle_no)
VALUES (1, 2, '진입 사다리', 3, 1, 2, 'N', NULL);

INSERT INTO PASSAGE (case_id, passage_no, passage_name, from_area_no, to_area_no, min_free_hands, requires_power, unlock_puzzle_no)
VALUES (1, 3, '보안실 문', 3, 4, 0, 'N', NULL);

INSERT INTO PASSAGE (case_id, passage_no, passage_name, from_area_no, to_area_no, min_free_hands, requires_power, unlock_puzzle_no)
VALUES (1, 4, '보안실 문', 4, 3, 0, 'N', NULL);

INSERT INTO PASSAGE (case_id, passage_no, passage_name, from_area_no, to_area_no, min_free_hands, requires_power, unlock_puzzle_no)
VALUES (1, 5, '제어실 문', 3, 5, 0, 'N', NULL);

INSERT INTO PASSAGE (case_id, passage_no, passage_name, from_area_no, to_area_no, min_free_hands, requires_power, unlock_puzzle_no)
VALUES (1, 6, '제어실 문', 5, 3, 0, 'N', NULL);

INSERT INTO PASSAGE (case_id, passage_no, passage_name, from_area_no, to_area_no, min_free_hands, requires_power, unlock_puzzle_no)
VALUES (1, 7, '옥상 탈출 사다리', 1, 2, 2, 'N', 3);

INSERT INTO PASSAGE (case_id, passage_no, passage_name, from_area_no, to_area_no, min_free_hands, requires_power, unlock_puzzle_no)
VALUES (1, 8, '옥상 탈출 사다리', 2, 1, 2, 'N', 3);

INSERT INTO PASSAGE (case_id, passage_no, passage_name, from_area_no, to_area_no, min_free_hands, requires_power, unlock_puzzle_no)
VALUES (1, 9, '화물 승강기', 3, 6, 0, 'Y', 3);

INSERT INTO PASSAGE (case_id, passage_no, passage_name, from_area_no, to_area_no, min_free_hands, requires_power, unlock_puzzle_no)
VALUES (1, 10, '화물 승강기', 6, 3, 0, 'Y', 3);

-- EVIDENCE 증거 정보와 원본·복사본 회수 조건
INSERT INTO EVIDENCE (case_id, evidence_no, area_no, evidence_name, evidence_note, original_hands, copy_allowed, unlock_puzzle_no)
VALUES (1, 1, 4, 'CCTV 기록 장치', '복사본에는 영상만 담긴다. CCTV 녹화 장치 원본에는 시계 보정 이력과 원본 서명도 남아 있다. 원본을 들면 양손이 차서 사다리를 이용할 수 없다.', 2, 'Y', 1);

INSERT INTO EVIDENCE (case_id, evidence_no, area_no, evidence_name, evidence_note, original_hands, copy_allowed, unlock_puzzle_no)
VALUES (1, 2, 4, '출입 명령 로그', '서버 로그에는 21:15의 출입 차단(B42)과 21:16의 봉쇄 승인(C09)이 남아 있다. 서버에 보관된 자료라 복사본만 가져갈 수 있다.', NULL, 'Y', 3);

-- ENDING 결말 내용과 우선순위
INSERT INTO ENDING (case_id, ending_no, ending_title, ending_text, ending_rank)
VALUES (1, 1, '기록은 남았지만', 'CCTV 기록은 무사히 가져왔다. 하지만 확보한 자료만으로는 출입문을 잠근 시각과 공식 승인 시각을 대조할 수 없었다.' || CHR(10) || '공식 보고서는 바뀌지 않았다. 대피 명단에서 지워진 세 사람의 이름은 내 수첩에만 남았다.', 10);

INSERT INTO ENDING (case_id, ending_no, ending_title, ending_text, ending_rank)
VALUES (1, 2, '닫힌 문을 연 기록', 'CCTV 원본의 시계 보정 이력과 출입 로그를 대조했다. 문은 21:15에 잠겼고, 봉쇄 승인은 1분 뒤에 내려졌다.' || CHR(10) || '회수반은 대피 보고서를 정정했다. 대피하지 못한 세 사람의 이름도 공식 기록에 다시 올라갔다.', 20);

-- ENDING_REQUIREMENT 결말에 필요한 증거와 회수 형태
INSERT INTO ENDING_REQUIREMENT (case_id, ending_no, evidence_no, required_form)
VALUES (1, 1, 1, 'ANY');

INSERT INTO ENDING_REQUIREMENT (case_id, ending_no, evidence_no, required_form)
VALUES (1, 2, 1, 'ORIGINAL');

INSERT INTO ENDING_REQUIREMENT (case_id, ending_no, evidence_no, required_form)
VALUES (1, 2, 2, 'COPY');

COMMIT;
PROMPT [DONE] Scenario inserted: 45 rows.
