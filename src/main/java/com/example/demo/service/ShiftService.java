package com.example.demo.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.demo.dto.UserProfileDto;
import com.example.demo.form.ShiftGenerationForm;
import com.example.demo.model.Shift;
import com.example.demo.model.Shift.Status;
import com.example.demo.model.UserProfile;
import com.example.demo.repository.ShiftRepository;
import com.example.demo.repository.UserProfileRepository;

@Service
public class ShiftService {

	private final ShiftRepository shiftRepository;
	private final UserProfileRepository userProfileRepository;

	public ShiftService(ShiftRepository shiftRepository,
			UserProfileRepository userProfileRepository) {
		this.shiftRepository = shiftRepository;
		this.userProfileRepository = userProfileRepository;
	}

	/**
	 * 指定したユーザーリスト・日付リスト・部署に対するシフト情報を取得し、
	 * Map<"userId_日付", 勤務種別>の形式で返却する。
	 *
	 * @param users ユーザー一覧（表示対象）
	 * @param dates 表示対象月の日付一覧
	 * @param department 所属部署名
	 * @return Map形式の勤務情報（セル表示用）
	 */
	public Map<String, String> getShiftMap(List<UserProfileDto> users, List<LocalDate> dates, String department) {

		// --- デバッグ: メソッドの入り口でパラメータを確認 ---
		System.out.println("[getShiftMap] START");
		System.out.println("[getShiftMap] department=" + department);
		System.out.println("[getShiftMap] dates=" + dates);
		System.out.println("[getShiftMap] users.size=" + (users != null ? users.size() : null));

		if (dates == null || dates.isEmpty()) {
			System.out.println("[getShiftMap] dates is null or empty -> return emptyMap");
			return Collections.emptyMap();
		}

		if (users == null || users.stream().allMatch(Objects::isNull)) {
			System.out.println("[getShiftMap] users is null or all null -> return emptyMap");
			return Collections.emptyMap();
		}

		// 日付リストをソート＋null除外したものを作る
		List<LocalDate> normalizedDates = dates.stream()
				.filter(Objects::nonNull)
				.sorted()
				.collect(Collectors.toList());

		if (normalizedDates.isEmpty()) {
			System.out.println("[getShiftMap] normalizedDates is empty -> return emptyMap");
			return Collections.emptyMap();
		}

		LocalDate start = normalizedDates.get(0);
		LocalDate end = normalizedDates.get(normalizedDates.size() - 1);

		// --- デバッグ: 日付範囲の確認 ---
		System.out.println("[getShiftMap] target date range: " + start + " ～ " + end);

		// --- 表示対象全体の日付＋部署のシフト情報をまとめて取得 ---
		// 旧）IN 版：findByDepartmentAndDateIn(department, dates);
		// 新）BETWEEN 版（両端含む）
		//
		// DRAFT と CONFIRMED の両方を取得し、
		// 1セルに複数レコードがある場合は
		//   優先順位: CONFIRMED > DRAFT
		//   同じステータス同士なら updatedAt が新しい方
		// で 1件に絞って表示に使う。
		List<Shift> shiftsDraft = shiftRepository.findByDepartmentAndDateBetweenAndStatus(
				department, start, end, Status.DRAFT);
		List<Shift> shiftsConfirmed = shiftRepository.findByDepartmentAndDateBetweenAndStatus(
				department, start, end, Status.CONFIRMED);

		// --- デバッグ: DB から取得した件数 ---
		System.out.println("[getShiftMap] shiftsDraft.size     = " + shiftsDraft.size());
		System.out.println("[getShiftMap] shiftsConfirmed.size = " + shiftsConfirmed.size());

		// 中身をざっくり確認したい場合（必要に応じて）
		for (Shift s : shiftsDraft) {
			System.out.println("[getShiftMap] DRAFT     userId=" + s.getUser().getId()
					+ ", date=" + s.getDate()
					+ ", type=" + s.getShiftType()
					+ ", status=" + s.getStatus()
					+ ", updatedAt=" + s.getUpdatedAt());
		}
		for (Shift s : shiftsConfirmed) {
			System.out.println("[getShiftMap] CONFIRMED userId=" + s.getUser().getId()
					+ ", date=" + s.getDate()
					+ ", type=" + s.getShiftType()
					+ ", status=" + s.getStatus()
					+ ", updatedAt=" + s.getUpdatedAt());
		}

		// 画面に表示する対象ユーザーIDの集合
		Set<Long> targetUserIds = users == null ? Set.of()
				: users.stream()
						.filter(Objects::nonNull)
						.map(UserProfileDto::getId)
						.filter(Objects::nonNull)
						.collect(Collectors.toSet());
		Set<LocalDate> targetDates = new HashSet<>(normalizedDates);

		System.out.println("[getShiftMap] targetUserIds = " + targetUserIds);
		System.out.println("[getShiftMap] targetDates.size = " + targetDates.size());

		// ▼ 同一セルに複数レコード（DRAFT と CONFIRMED）が存在する場合に備えて
		//    最新の状態（優先度: CONFIRMED > DRAFT、同一優先度では更新日時が新しい方）を採用する。
		Map<String, Shift> latestByCell = new HashMap<>();
		for (Shift shift : shiftsDraft) {
			mergeShiftIfNecessary(latestByCell, targetUserIds, targetDates, shift);
		}
		for (Shift shift : shiftsConfirmed) {
			mergeShiftIfNecessary(latestByCell, targetUserIds, targetDates, shift);
		}

		System.out.println("[getShiftMap] latestByCell.size=" + latestByCell.size());

		Map<String, String> map = new HashMap<>();

		// 最終的に Thymeleaf に渡す Map を作成
		for (Shift shift : latestByCell.values()) {
			Long userId = shift.getUser().getId();
			LocalDate date = shift.getDate();
			String shiftType = shift.getShiftType();

			if (!StringUtils.hasText(shiftType)) {
				// 空文字や null は画面に表示しない（既存の値を上書きしない）
				continue;
			}

			String key = userId + "_" + date;
			map.put(key, shiftType.trim());
		}

		System.out.println("[getShiftMap] result map size=" + map.size());
		// 特定のセルを確認したい場合（例: userId=6, 2025-12-01）
		System.out.println("[getShiftMap] result[6_2025-12-01] = " + map.get("6_2025-12-01"));

		System.out.println("[getShiftMap] END");
		return map;
	}

	private void mergeShiftIfNecessary(Map<String, Shift> latestByCell,
			Set<Long> targetUserIds,
			Set<LocalDate> targetDates,
			Shift shift) {
		if (shift == null || shift.getUser() == null || shift.getDate() == null) {
			return;
		}

		Long userId = shift.getUser().getId();
		LocalDate date = shift.getDate();

		// 対象ユーザー／対象日付以外は無視
		if (!targetUserIds.isEmpty() && (userId == null || !targetUserIds.contains(userId))) {
			return;
		}
		if (!targetDates.contains(date)) {
			return;
		}

		String key = userId + "_" + date;
		Shift current = latestByCell.get(key);

		// --- デバッグ: マージ前の状態を確認 ---
		System.out.println("[mergeShiftIfNecessary] key=" + key
				+ ", incomingStatus=" + shift.getStatus()
				+ ", currentStatus=" + (current != null ? current.getStatus() : null));

		if (current == null || isPreferred(shift, current)) {
			latestByCell.put(key, shift);
			System.out.println("[mergeShiftIfNecessary] -> put/replace. key=" + key
					+ ", usedStatus=" + shift.getStatus());
		} else {
			System.out.println("[mergeShiftIfNecessary] -> keep current. key=" + key);
		}
	}

	/**
	 * 2つのシフトのうち、画面表示として優先すべき方を判定する。
	 * 優先順位:
	 *  1. Status が CONFIRMED の方を優先
	 *  2. Status が DRAFT の方を次点で優先
	 *  3. Status が同じ場合は updatedAt が新しい方を優先
	 */
	private boolean isPreferred(Shift candidate, Shift current) {
		Status candidateStatus = candidate.getStatus();
		Status currentStatus = current.getStatus();

		int candidatePriority = statusPriority(candidateStatus);
		int currentPriority = statusPriority(currentStatus);

		if (candidatePriority != currentPriority) {
			return candidatePriority > currentPriority;
		}

		LocalDateTime candidateUpdated = candidate.getUpdatedAt();
		LocalDateTime currentUpdated = current.getUpdatedAt();

		if (candidateUpdated == null) {
			return false;
		}
		if (currentUpdated == null) {
			return true;
		}

		return candidateUpdated.isAfter(currentUpdated);
	}

	/**
	 * ステータスごとの優先度
	 * CONFIRMED(2) > DRAFT(1) > その他(0)
	 */
	private int statusPriority(Status status) {
		if (status == Status.CONFIRMED) {
			return 2;
		}
		if (status == Status.DRAFT) {
			return 1;
		}
		return 0;
	}

	// =====================================================
	// シフトの保存・更新関連メソッド
	// =====================================================

	/**
	 * 画面の入力データを一括で保存する。
	 *
	 * - status = DRAFT（一時保存） または CONFIRMED（確定）
	 * - Map の key は "userId_YYYY-MM-DD" というフォーマットを想定
	 * - 値が "-" または 空文字("") のセルは「シフトなし」とみなして DELETE
	 * - それ以外の値は INSERT or UPDATE（Upsert）
	 */
	@Transactional
	public void saveShifts(ShiftGenerationForm form, Status status) {
		if (form == null || form.getShifts() == null || form.getShifts().isEmpty()) {
			return;
		}
		if (!StringUtils.hasText(form.getDepartment())) {
			throw new IllegalArgumentException("Department must not be null or empty when saving shifts");
		}
		if (form.getTargetMonth() == null) {
			throw new IllegalArgumentException("TargetMonth must not be null when saving shifts");
		}
		if (status != Status.DRAFT && status != Status.CONFIRMED) {
			throw new IllegalArgumentException("Status must be DRAFT or CONFIRMED when saving shifts");
		}

		String department = form.getDepartment().trim();
		YearMonth targetMonth = form.getTargetMonth();
		LocalDate startDate = targetMonth.atDay(1);
		LocalDate endDate = targetMonth.atEndOfMonth();

		// 無効職員は、対象部署・対象月に保存済みシフトがある場合だけ
		// 過去シフトの手動訂正対象として許可する。
		Set<Long> inactiveHistoryUserIds = shiftRepository
				.findByDepartmentAndDateBetween(department, startDate, endDate)
				.stream()
				.filter(Objects::nonNull)
				.map(Shift::getUser)
				.filter(Objects::nonNull)
				.filter(user -> Boolean.FALSE.equals(user.getActive()))
				.map(UserProfile::getId)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		List<ValidatedShiftCell> cells = new ArrayList<>();

		// 1件でも不正なセルがあれば、DBを更新する前に処理全体を中止する。
		for (Map.Entry<String, String> entry : form.getShifts().entrySet()) {
			ValidatedShiftCell cell = validateShiftCell(
					entry,
					department,
					targetMonth,
					inactiveHistoryUserIds);
			cells.add(cell);
		}

		// 全セルの検証に成功してから、削除・登録・更新を開始する。
		for (ValidatedShiftCell cell : cells) {
			if (cell.value().isEmpty() || "-".equals(cell.value())) {
				shiftRepository.deleteByUser_IdAndDateAndDepartment(
						cell.user().getId(), cell.date(), department);
				continue;
			}

			Shift shift = shiftRepository
					.findByUser_IdAndDateAndDepartment(
							cell.user().getId(), cell.date(), department)
					.orElseGet(Shift::new);

			if (shift.getId() == null) {
				shift.setUser(cell.user());
				shift.setDate(cell.date());
				shift.setDepartment(department);
			}

			shift.setShiftType(cell.value());
			shift.setStatus(status);
			// 画面から保存した値は、既存行がAUTOでも手動訂正として保護する。
			shift.setSourceType(Shift.SourceType.MANUAL);
			shiftRepository.save(shift);
		}
	}

	/**
	 * 保存対象の1セルを検証する。
	 * 有効職員は現在の所属部署が一致する場合に許可する。
	 * 無効職員は対象部署・対象月に保存済み履歴がある場合だけ許可する。
	 * 存在しない職員・対象外職員・対象月外は保存も削除も許可しない。
	 */
	private ValidatedShiftCell validateShiftCell(
			Map.Entry<String, String> entry,
			String department,
			YearMonth targetMonth,
			Set<Long> inactiveHistoryUserIds) {

		String key = entry.getKey();
		if (!StringUtils.hasText(key)) {
			throw new IllegalArgumentException("Shift key must not be empty");
		}

		int separator = key.indexOf('_');
		if (separator <= 0 || separator != key.lastIndexOf('_')) {
			throw new IllegalArgumentException("Invalid shift key: " + key);
		}

		Long userId;
		LocalDate date;
		try {
			userId = Long.valueOf(key.substring(0, separator));
			date = LocalDate.parse(key.substring(separator + 1));
		} catch (RuntimeException e) {
			throw new IllegalArgumentException("Invalid shift key: " + key, e);
		}

		if (!key.equals(userId + "_" + date)) {
			throw new IllegalArgumentException("Non-canonical shift key: " + key);
		}
		if (!targetMonth.equals(YearMonth.from(date))) {
			throw new IllegalArgumentException("Shift date is outside target month: " + key);
		}

		UserProfile user = userProfileRepository.findById(userId)
				.orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

		if (Boolean.TRUE.equals(user.getActive())) {
			if (!department.equalsIgnoreCase(user.getDepartment())) {
				throw new IllegalArgumentException("User does not belong to department: " + userId);
			}
		} else if (!inactiveHistoryUserIds.contains(userId)) {
			throw new IllegalArgumentException(
					"Inactive user has no shift history in target department and month: " + userId);
		}

		String value = entry.getValue() == null ? "" : entry.getValue().trim();
		return new ValidatedShiftCell(user, date, value);
	}

	/** 検証済みセル。検証後の値だけを更新処理へ渡す。 */
	private record ValidatedShiftCell(UserProfile user, LocalDate date, String value) {
	}

	/**
	 * 確定解除：指定範囲のシフトをすべて DRAFT に戻す。
	 *
	 * ▼仕様
	 * - 対象：フォームの「部署」＋「対象月」に属するシフト
	 * - 現在 CONFIRMED のものだけを DRAFT に変更する
	 * - DRAFT の行はそのまま（変更しない）
	 *
	 * 「一度確定した月を、もう一度編集可能な下書き状態に戻す」ための操作。
	 */
	@Transactional
	public void unconfirmShifts(ShiftGenerationForm form) {

		// フォームが null なら何もしない
		if (form == null) {
			return;
		}

		// 部署必須チェック
		if (!StringUtils.hasText(form.getDepartment())) {
			throw new IllegalArgumentException("Department must not be null or empty when unconfirming shifts");
		}
		final String department = form.getDepartment().trim();

		// 対象月が設定されている前提で処理
		YearMonth targetMonth = form.getTargetMonth();
		if (targetMonth == null) {
			// 万が一 targetMonth が入っていない場合は、従来どおり
			// form.shifts のキーから日付を1件ずつ unconfirm する方法にフォールバックしてもよいが、
			// ここでは明示的に例外にしておく（運用上、必ず targetMonth がセットされる想定）
			throw new IllegalArgumentException("TargetMonth must not be null when unconfirming shifts");
		}

		LocalDate start = targetMonth.atDay(1);
		LocalDate end = targetMonth.atEndOfMonth();

		// 対象部署・対象月の CONFIRMED シフトを一括取得
		List<Shift> confirmedList = shiftRepository.findByDepartmentAndDateBetweenAndStatus(
				department, start, end, Status.CONFIRMED);

		System.out.println("DEBUG unconfirmShifts: dept=" + department
				+ ", month=" + targetMonth
				+ ", confirmedCount=" + confirmedList.size());

		// 更新開始前に全件を検証し、途中まで確定解除されることを防ぐ。
		for (Shift shift : confirmedList) {
			UserProfile user = shift.getUser();
			if (user == null || user.getId() == null) {
				throw new IllegalArgumentException("Confirmed shift has no user");
			}
			// 無効職員は、この検索結果自体が対象部署・対象月の履歴であるため許可する。
			// 現在も有効な職員は、現在の所属部署が一致する場合だけ許可する。
			if (Boolean.TRUE.equals(user.getActive())
					&& !department.equalsIgnoreCase(user.getDepartment())) {
				throw new IllegalArgumentException(
						"Active user does not belong to department: " + user.getId());
			}
		}

		// 全件の検証後に、対象部署・対象月の有効職員と無効職員を
		// CONFIRMEDからDRAFTへ変更する。
		for (Shift shift : confirmedList) {
			shift.setStatus(Status.DRAFT);
			shiftRepository.save(shift);
		}
	}

	// -----------------------------------------------------
	// （参考）状態で絞りたい場合の BETWEEN 版サンプル（必要になったら有効化）
	// public Map<String, String> getShiftMapByStatus(
	//         List<UserProfileDto> users, List<LocalDate> dates, String department, Status status) {
	//     if (dates == null || dates.isEmpty()) return Map.of();
	//     LocalDate start = Collections.min(dates);
	//     LocalDate end   = Collections.max(dates);
	//     List<Shift> shifts = shiftRepository.findByDepartmentAndDateBetweenAndStatus(department, start, end, status);
	//     Map<String, String> map = new HashMap<>();
	//     for (Shift shift : shifts) {
	//         String key = shift.getUser().getId() + "_" + shift.getDate();
	//         map.put(key, shift.getShiftType());
	//     }
	//     return map;
	// }
	// -----------------------------------------------------
}
