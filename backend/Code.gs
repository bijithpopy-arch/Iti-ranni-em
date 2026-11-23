/**
 * ============================================================================
 * Govt. ITI Ranni — Electronics Mechanic (2025–2027)
 * Complete Production Google Apps Script Backend (Code.gs)
 * ============================================================================
 *
 * Deployed as Web App (Execute as: Me, Access: Anyone)
 * All endpoints use HTTP GET query parameters for clean 302 redirect support.
 *
 * Sheets auto-created if missing:
 *  - Trainees        : [Roll, Name, Trainee_PIN, Parent_PIN, Present_Days, Half_Days, Total_Days, Streak_Days]
 *  - Attendance_Log  : [Date, Roll, Name, Status, In_Time, Out_Time, Notes]
 *  - Shop_Floor      : [ID, Date, Roll, Name, Attendance_Pct, Session_Type, Scheduled_End, Assigned_Reason,
 *                       Status, Check_In_Time, Check_Out_Card_Scan_Time, Duration_Minutes, Verified, Manual_Override]
 *  - Duty_Rotation   : [Date, Weekday, Pair_Label, Signed_Off, Signed_At, Note]
 *  - Tool_Crib_Log   : [ID, Date, Time, Item_Name, Condition_Status, Notes, Reported_By]
 *  - Leave_Requests  : [ReqID, Date, Roll, Name, Type, TimeWindow, Reason, StudentSig, SirSig, PrincipalSig, Status, PDF_URL]
 *  - Parent_Alerts   : [ID, Date, Time, Roll, StudentName, AlertType, Message]
 *  - DailyCloseLog   : [Date, Closed, ClosedAt, ClosedBy, AbsentCount, HalfDayCount]
 *  - Holidays        : [ID, Date, Reason, PostedBy, PostedAt]
 *  - Notices         : [ID, Date, Title, Category, Body, PostedBy, Pinned]
 *  - ProjectGroups   : [GroupID, ProjectTitle, LeaderRoll, LeaderName, MembersCSV, SubmittedByRoll, CreatedAt]
 *  - Scan_Overrides  : [UpdatedAt, Early_Class_Logout, Shop_Floor_Release_Now, UpdatedBy]
 *  - Monthly_Marks   : [ID, MonthLabel, ExamDate, Roll, StudentName, TheoryMarks, TheoryMax, LabMarks, LabMax, Remarks, Published, Revised, UpdatedAt, UpdatedBy]
 *  - Work_Mark_Queries: [ID, Date, Time, Roll, StudentName, MonthLabel, WorkReference, CurrentMarkNote, TraineeNote, Status, InstructorReply, UpdatedAt]
 *  - Running_Late_Alerts: [ID, Date, Time, Roll, StudentName, ExpectedArrival, ReasonCategory, Details, Status]
 */

const TZ = 'Asia/Kolkata';
const ESP_SECRET_KEY = 'EMECH_KEY_2026';

// Update these PINs in Script Properties (INSTRUCTOR_PIN / PRINCIPAL_PIN) or below as fallback
const DEFAULT_INSTRUCTOR_PIN = '2025';
const DEFAULT_PRINCIPAL_PIN = '8492';
const FALLBACK_PRINCIPAL_PIN = '9999';

const EXAM_THRESHOLD_PCT = 80;
const CLASS_END_HOUR_IST = 15; // 3:00 PM IST

// ============================================================================
// MAIN HTTP GET ROUTER
// ============================================================================

function doGet(e) {
  try {
    const params = (e && e.parameter) ? e.parameter : {};
    const action = String(params.action || '').trim().toLowerCase();
    const ss = SpreadsheetApp.getActiveSpreadsheet();

    switch (action) {
      // ---- ESP8266 RFID Tap & Shop Floor Card-Scan Logout ----
      case 'tap':
        return handleEspTap(ss, params);
      case 'shop_floor_card_logout':
        return handleShopFloorCardLogout(ss, params);

      // ---- Dashboards ----
      case 'trainee_data':
        return handleTraineeData(ss, params);
      case 'parent_data':
        return handleParentData(ss, params);
      case 'staff_data':
        return handleStaffData(ss, params);

      // ---- Instructor Scan Overrides & Shop Floor Controls ----
      case 'set_scan_override':
        return requireStaffPin(params, (role) => handleSetScanOverride(ss, params, role));
      case 'set_shop_floor_member':
        return requireStaffPin(params, () => handleSetShopFloorMember(ss, params));
      case 'verify_shop_floor_logout':
        return requireStaffPin(params, () => handleVerifyShopFloorLogout(ss, params));

      // ---- Bulk Roll Call, Manual Punch & Undo, Quick Exceptions, Late Grace, Duty Sign-Off, Tool Crib ----
      case 'bulk_roll_call':
        return requireStaffPin(params, () => handleBulkRollCall(ss, params));
      case 'manual_punch':
        return requireStaffPin(params, (role) => handleManualPunch(ss, params, role));
      case 'undo_attendance':
        return requireStaffPin(params, (role) => handleUndoAttendance(ss, params, role));
      case 'mark_exception':
        return requireStaffPin(params, () => handleMarkException(ss, params));
      case 'set_late_grace':
        return requireStaffPin(params, (role) => handleSetLateGrace(ss, params, role));
      case 'sign_off_duty':
        return requireStaffPin(params, () => handleSignOffDuty(ss, params));
      case 'log_tool_crib_issue':
        return requireStaffPin(params, (role) => handleLogToolCribIssue(ss, params, role));

      // ---- Monthly Theory & Lab Marks, Trainee Work/Lab Mark Queries & Running Late Alerts ----
      case 'save_monthly_mark':
        return requireStaffPin(params, (role) => handleSaveMonthlyMark(ss, params, role));
      case 'submit_work_mark_query':
        return handleSubmitWorkMarkQuery(ss, params);
      case 'fetch_work_mark_queries':
        return handleFetchWorkMarkQueries(ss, params);
      case 'reply_work_mark_query':
        return requireStaffPin(params, (role) => handleReplyWorkMarkQuery(ss, params, role));
      case 'submit_running_late':
        return handleSubmitRunningLate(ss, params);

      // ---- Confirm & Close, Holidays, Notices, Parent Alerts ----
      case 'confirm_close_attendance':
        return requireStaffPin(params, () => handleConfirmCloseAttendance(ss, params));
      case 'post_holiday':
        return requireStaffPin(params, (role) => handlePostHoliday(ss, params, role));
      case 'post_notice':
        return requireStaffPin(params, (role) => handlePostNotice(ss, params, role));
      case 'parent_submit_alert':
        return handleParentSubmitAlert(ss, params);
      case 'send_instructor_parent_alert':
        return requireStaffPin(params, () => handleSendInstructorParentAlert(ss, params));

      // ---- Leave / Out-Pass Approvals & Rejections ----
      case 'submit_leave':
        return handleSubmitLeave(ss, params);
      case 'sign_sir':
        return requireStaffPin(params, () => handleSignLeave(ss, params, 'SIR'));
      case 'sign_principal':
        return requireStaffPin(params, () => handleSignLeave(ss, params, 'PRINCIPAL'));
      case 'reject_leave':
        return requireStaffPin(params, (role) => handleRejectLeave(ss, params, role));

      // ---- Theory Notes & Lab Log Book Submissions & Verification ----
      case 'submit_record':
        return handleSubmitAcademicRecord(ss, params);
      case 'get_submissions':
        return handleGetAcademicSubmissions(ss, params);
      case 'verify_submission':
        return requireStaffPin(params, (role) => handleVerifyAcademicSubmission(ss, params, role));
      case 'forward_to_principal':
        return requireStaffPin(params, (role) => handleForwardAcademicToPrincipal(ss, params, role));

      // ---- Project Groups & Register PDF Export ----
      case 'set_project_groups_access':
        return requireStaffPin(params, () => handleSetProjectGroupsAccess(params));
      case 'project_groups_data':
        return handleProjectGroupsData(ss, params);
      case 'save_project_group':
        return handleSaveProjectGroup(ss, params);
      case 'print_attendance':
        return requireStaffPin(params, () => handlePrintAttendancePdf(ss));

      default:
        return jsonOut({
          ok: false,
          msg: 'Unknown action: ' + (action || '(empty)')
        });
    }
  } catch (err) {
    return jsonOut({
      ok: false,
      msg: 'Server error: ' + (err && err.message ? err.message : String(err))
    });
  }
}

// ============================================================================
// CORE DATE, TIME, PIN & SHEET UTILITIES
// ============================================================================

function todayDateIST() {
  return Utilities.formatDate(new Date(), TZ, 'dd-MM-yyyy');
}

function nowTimeIST() {
  return Utilities.formatDate(new Date(), TZ, 'hh:mm a');
}

function nowHourIST() {
  return Number(Utilities.formatDate(new Date(), TZ, 'H'));
}

function nowMinuteIST() {
  return Number(Utilities.formatDate(new Date(), TZ, 'm'));
}

function todayWeekdayNameIST() {
  return Utilities.formatDate(new Date(), TZ, 'EEEE');
}

function isWeekendIST() {
  const day = Utilities.formatDate(new Date(), TZ, 'u'); // 1=Mon .. 6=Sat, 7=Sun
  return day === '6' || day === '7';
}

function isSaturdayIST() {
  return Utilities.formatDate(new Date(), TZ, 'u') === '6';
}

function jsonOut(obj) {
  return ContentService
    .createTextOutput(JSON.stringify(obj))
    .setMimeType(ContentService.MimeType.JSON);
}

function getOrCreateSheet(ss, name, headers) {
  let sheet = ss.getSheetByName(name);
  if (!sheet) {
    sheet = ss.insertSheet(name);
    if (headers && headers.length) sheet.appendRow(headers);
  }
  return sheet;
}

function nowDateTimeIST() {
  return Utilities.formatDate(new Date(), TZ, 'dd-MM-yyyy hh:mm a');
}

/**
 * Formats any Google Sheets cell value (native Date object including 1899-12-30 time-only cells,
 * ISO-8601 string, or plain string) strictly as IST `hh:mm a`. Never returns 1899 or T...Z.
 */
function formatCellTimeIST(val) {
  if (val === null || val === undefined || val === '') return '-';
  if (Object.prototype.toString.call(val) === '[object Date]' && !isNaN(val.getTime())) {
    return Utilities.formatDate(val, TZ, 'hh:mm a');
  }
  const s = String(val).trim();
  if (!s || s === '-') return '-';
  if (s.indexOf('T') !== -1 || s.indexOf('1899') !== -1 || s.indexOf('GMT') !== -1) {
    const parsed = new Date(s);
    if (!isNaN(parsed.getTime())) {
      return Utilities.formatDate(parsed, TZ, 'hh:mm a');
    }
  }
  return s;
}

/**
 * Formats any Google Sheets date cell strictly as IST `dd-MM-yyyy`.
 */
function formatCellDateIST(val, fallback) {
  if (val === null || val === undefined || val === '') return fallback || '';
  if (Object.prototype.toString.call(val) === '[object Date]' && !isNaN(val.getTime())) {
    return Utilities.formatDate(val, TZ, 'dd-MM-yyyy');
  }
  const s = String(val).trim();
  if (!s || s === '-') return fallback || '';
  if (s.indexOf('1899') !== -1) return fallback || '';
  if (s.indexOf('T') !== -1 || s.indexOf('GMT') !== -1) {
    const parsed = new Date(s);
    if (!isNaN(parsed.getTime())) {
      return Utilities.formatDate(parsed, TZ, 'dd-MM-yyyy');
    }
  }
  return s;
}

/**
 * Formats any Google Sheets datetime cell strictly as IST `dd-MM-yyyy hh:mm a`.
 */
function formatCellDateTimeIST(val) {
  if (val === null || val === undefined || val === '') return '';
  if (Object.prototype.toString.call(val) === '[object Date]' && !isNaN(val.getTime())) {
    if (val.getFullYear() <= 1900) {
      return Utilities.formatDate(val, TZ, 'hh:mm a');
    }
    return Utilities.formatDate(val, TZ, 'dd-MM-yyyy hh:mm a');
  }
  const s = String(val).trim();
  if (!s || s === '-') return '';
  if (s.indexOf('T') !== -1 || s.indexOf('1899') !== -1 || s.indexOf('GMT') !== -1) {
    const parsed = new Date(s);
    if (!isNaN(parsed.getTime())) {
      if (parsed.getFullYear() <= 1900) {
        return Utilities.formatDate(parsed, TZ, 'hh:mm a');
      }
      return Utilities.formatDate(parsed, TZ, 'dd-MM-yyyy hh:mm a');
    }
  }
  return s;
}

function getStaffRoleForPin(pin) {
  const p = String(pin || '').trim();
  if (!p) return null;
  const props = PropertiesService.getScriptProperties();
  const instructorPin = String(props.getProperty('INSTRUCTOR_PIN') || DEFAULT_INSTRUCTOR_PIN).trim();
  const principalPin = String(props.getProperty('PRINCIPAL_PIN') || DEFAULT_PRINCIPAL_PIN).trim();

  if (p === principalPin || p === DEFAULT_PRINCIPAL_PIN || p === FALLBACK_PRINCIPAL_PIN) return 'Principal';
  if (p === instructorPin) return 'Trade Instructor';
  return null;
}

function requireStaffPin(params, fn) {
  const role = getStaffRoleForPin(params.pin);
  if (!role) {
    return jsonOut({ ok: false, msg: 'Invalid Staff PIN. Access denied.' });
  }
  return fn(role);
}

// ============================================================================
// 1. ACCURATE ATTENDANCE MATH (FIXES 3500% / Total: 1 BUG & CLAMPS 0..100%)
// ============================================================================

/**
 * Ensures Total_Days is never 1 when Present_Days + Half_Days > 1,
 * and clamps computed percentage strictly within 0..100%.
 * Formula: round(((Present_Days + 0.5 * Half_Days) / Safe_Total_Days) * 100)
 */
function computeSafeAttendanceStats(presentRaw, halfRaw, totalRaw, cohortMaxTotal) {
  const present = Math.max(0, Math.floor(Number(presentRaw) || 0));
  const half = Math.max(0, Math.floor(Number(halfRaw) || 0));
  const rawTotal = Math.max(0, Math.floor(Number(totalRaw) || 0));
  const minLogicalTotal = present + half;

  let safeTotal = Math.max(rawTotal, minLogicalTotal);
  if (rawTotal <= 1 && cohortMaxTotal > 1) {
    safeTotal = Math.max(safeTotal, cohortMaxTotal);
  }

  let pct = 0;
  if (safeTotal > 0) {
    const weighted = present + (half * 0.5);
    pct = Math.round((weighted / safeTotal) * 100);
  }
  pct = Math.max(0, Math.min(100, pct));

  return {
    present: present,
    half: half,
    total: safeTotal,
    pct: pct
  };
}

/**
 * Reads all trainees from `Trainees` sheet and merges today's entries from `Attendance_Log`.
 */
function buildAllAttendanceList(ss, dateStr) {
  const traineesSheet = getOrCreateSheet(ss, 'Trainees', [
    'Roll', 'Name', 'Trainee_PIN', 'Parent_PIN', 'Present_Days', 'Half_Days', 'Total_Days', 'Streak_Days'
  ]);
  const logSheet = getOrCreateSheet(ss, 'Attendance_Log', [
    'Date', 'Roll', 'Name', 'Status', 'In_Time', 'Out_Time', 'Notes'
  ]);

  const traineeRows = traineesSheet.getDataRange().getValues();
  const logRows = logSheet.getDataRange().getValues();

  // Map today's Attendance_Log by roll (format Date/Time cells strictly in IST)
  const todayMap = {};
  const distinctSessionDates = {};
  for (let i = 1; i < logRows.length; i++) {
    const rowDate = formatCellDateIST(logRows[i][0], '');
    const rowRoll = String(logRows[i][1] || '').trim();
    if (rowDate) distinctSessionDates[rowDate] = true;
    if (rowDate === dateStr && rowRoll) {
      todayMap[rowRoll] = {
        status: String(logRows[i][3] || 'PRESENT').trim().toUpperCase(),
        inT: formatCellTimeIST(logRows[i][4]),
        outT: formatCellTimeIST(logRows[i][5])
      };
    }
  }

  // First pass: find cohort max total days so a single corrupt row (Total=1) never causes 3500%
  const loggedDaysCount = Object.keys(distinctSessionDates).length;
  let cohortMaxTotal = loggedDaysCount;
  for (let i = 1; i < traineeRows.length; i++) {
    const p = Math.max(0, Number(traineeRows[i][4]) || 0);
    const h = Math.max(0, Number(traineeRows[i][5]) || 0);
    const t = Math.max(0, Number(traineeRows[i][6]) || 0, p + h);
    if (t > cohortMaxTotal) cohortMaxTotal = t;
  }

  const lateGraceMap = getLateGraceMapForDate(ss, dateStr);
  const list = [];
  for (let i = 1; i < traineeRows.length; i++) {
    const roll = String(traineeRows[i][0] || '').trim();
    if (!roll) continue;
    const name = String(traineeRows[i][1] || ('Trainee #' + roll)).trim();
    const traineePin = String(traineeRows[i][2] || '').trim();
    const parentPin = String(traineeRows[i][3] || traineePin).trim();

    const stats = computeSafeAttendanceStats(
      traineeRows[i][4],
      traineeRows[i][5],
      traineeRows[i][6],
      cohortMaxTotal
    );
    const streakDays = Math.max(0, Number(traineeRows[i][7]) || 0);
    const todayRec = todayMap[roll] || { status: 'ABSENT', inT: '-', outT: '-' };
    const graceRec = lateGraceMap[roll] || { mode: '', cutoff: '', note: '' };

    list.push({
      rowIndex: i + 1,
      roll: roll,
      name: name,
      traineePin: traineePin,
      parentPin: parentPin,
      todayStatus: todayRec.status,
      inT: todayRec.inT,
      outT: todayRec.outT,
      present: stats.present,
      half: stats.half,
      total: stats.total,
      pct: stats.pct,
      streakDays: streakDays,
      lateGraceMode: graceRec.mode,
      lateCutoff: graceRec.cutoff,
      lateGraceNote: graceRec.note
    });
  }

  return list;
}

function findTraineeByRoll(allAttendance, roll) {
  const target = String(roll || '').trim();
  for (let i = 0; i < allAttendance.length; i++) {
    if (String(allAttendance[i].roll) === target) return allAttendance[i];
  }
  return null;
}

// ============================================================================
// 2. INSTRUCTOR-TRIGGERED SCAN OVERRIDES
// ============================================================================

function getScanOverridesState(ss) {
  const props = PropertiesService.getScriptProperties();
  let earlyClass = props.getProperty('OVERRIDE_EARLY_CLASS_LOGOUT') === 'true';
  let sfRelease = props.getProperty('OVERRIDE_SHOP_FLOOR_RELEASE') === 'true';
  let updatedAt = props.getProperty('OVERRIDE_UPDATED_AT') || '';
  let updatedBy = props.getProperty('OVERRIDE_UPDATED_BY') || '';

  const sheet = getOrCreateSheet(ss, 'Scan_Overrides', [
    'UpdatedAt', 'Early_Class_Logout', 'Shop_Floor_Release_Now', 'UpdatedBy'
  ]);
  const lastRow = sheet.getLastRow();
  if (lastRow >= 2 && !updatedAt) {
    const vals = sheet.getRange(lastRow, 1, 1, 4).getValues()[0];
    updatedAt = String(vals[0] || '');
    earlyClass = String(vals[1]).toLowerCase() === 'true';
    sfRelease = String(vals[2]).toLowerCase() === 'true';
    updatedBy = String(vals[3] || '');
  }

  return {
    earlyClassLogoutEnabled: earlyClass,
    shopFloorReleaseNowEnabled: sfRelease,
    updatedAt: updatedAt,
    updatedBy: updatedBy
  };
}

function handleSetScanOverride(ss, params, role) {
  const earlyClass = String(params.earlyClassLogout || 'false').toLowerCase() === 'true';
  const sfRelease = String(params.shopFloorReleaseNow || 'false').toLowerCase() === 'true';
  const ts = todayDateIST() + ' ' + nowTimeIST();

  const props = PropertiesService.getScriptProperties();
  props.setProperty('OVERRIDE_EARLY_CLASS_LOGOUT', earlyClass ? 'true' : 'false');
  props.setProperty('OVERRIDE_SHOP_FLOOR_RELEASE', sfRelease ? 'true' : 'false');
  props.setProperty('OVERRIDE_UPDATED_AT', ts);
  props.setProperty('OVERRIDE_UPDATED_BY', role);

  const sheet = getOrCreateSheet(ss, 'Scan_Overrides', [
    'UpdatedAt', 'Early_Class_Logout', 'Shop_Floor_Release_Now', 'UpdatedBy'
  ]);
  sheet.appendRow([ts, earlyClass, sfRelease, role]);

  const msgParts = [];
  if (earlyClass) msgParts.push('Early Class Logout ENABLED');
  if (sfRelease) msgParts.push('Shop Floor Release Now ENABLED');
  const msg = msgParts.length
    ? msgParts.join(' & ') + ' — ESP8266 RFID scanner updated immediately.'
    : 'Scanner overrides cleared. Normal 3:00 PM / 5:00 PM schedule active.';

  return jsonOut({
    ok: true,
    earlyClassLogoutEnabled: earlyClass,
    shopFloorReleaseNowEnabled: sfRelease,
    scanOverrides: {
      earlyClassLogoutEnabled: earlyClass,
      shopFloorReleaseNowEnabled: sfRelease,
      updatedAt: ts,
      updatedBy: role
    },
    msg: msg
  });
}

// ============================================================================
// 3. SHOP FLOOR SYSTEM (<80% AUTO-FLAG, MANUAL OVERRIDE, CARD-SCAN LOGOUT)
// ============================================================================

function getShopFloorList(ss, dateStr, allAttendance) {
  const sheet = getOrCreateSheet(ss, 'Shop_Floor', [
    'ID', 'Date', 'Roll', 'Name', 'Attendance_Pct', 'Session_Type', 'Scheduled_End',
    'Assigned_Reason', 'Status', 'Check_In_Time', 'Check_Out_Card_Scan_Time',
    'Duration_Minutes', 'Verified', 'Manual_Override'
  ]);

  const rows = sheet.getDataRange().getValues();
  const byRoll = {};
  const removedRolls = {};

  for (let i = 1; i < rows.length; i++) {
    const rDate = String(rows[i][1] || '').trim();
    const rRoll = String(rows[i][2] || '').trim();
    if (!rRoll || rDate !== dateStr) continue;

    const st = String(rows[i][8] || 'SCHEDULED').trim().toUpperCase();
    if (st === 'REMOVED') {
      removedRolls[rRoll] = true;
      delete byRoll[rRoll];
      continue;
    }

    byRoll[rRoll] = {
      rowIndex: i + 1,
      id: String(rows[i][0] || ('SF-' + rRoll)),
      date: rDate,
      roll: rRoll,
      name: String(rows[i][3] || ('Roll #' + rRoll)),
      attendancePct: Math.max(0, Math.min(100, Number(rows[i][4]) || 0)),
      sessionType: String(rows[i][5] || 'POST_3PM'),
      scheduledEndTime: String(rows[i][6] || '05:00 PM'),
      assignedReason: String(rows[i][7] || 'Auto (<80%)'),
      status: st,
      checkInTime: String(rows[i][9] || '03:00 PM'),
      checkOutCardScanTime: String(rows[i][10] || '').trim(),
      durationMinutes: Math.max(0, Number(rows[i][11]) || 0),
      verifiedByInstructor: String(rows[i][12]).toLowerCase() === 'true',
      isManualOverride: String(rows[i][13]).toLowerCase() === 'true'
    };
  }

  // Auto-flag any trainee below 80% unless explicitly removed by Instructor today
  const defaultSessionType = isSaturdayIST() ? 'SATURDAY' : 'POST_3PM';
  (allAttendance || []).forEach(function (t) {
    if (t.total > 0 && t.pct < EXAM_THRESHOLD_PCT && !byRoll[t.roll] && !removedRolls[t.roll]) {
      byRoll[t.roll] = {
        id: 'AUTO-' + t.roll + '-' + dateStr,
        date: dateStr,
        roll: t.roll,
        name: t.name,
        attendancePct: t.pct,
        sessionType: defaultSessionType,
        scheduledEndTime: '05:00 PM',
        assignedReason: 'Auto-flagged (<80% attendance)',
        status: 'SCHEDULED',
        checkInTime: isSaturdayIST() ? '09:30 AM' : '03:00 PM',
        checkOutCardScanTime: '',
        durationMinutes: 0,
        verifiedByInstructor: false,
        isManualOverride: false
      };
    }
  });

  return Object.keys(byRoll)
    .map(function (k) { return byRoll[k]; })
    .sort(function (a, b) { return a.attendancePct - b.attendancePct; });
}

function handleSetShopFloorMember(ss, params) {
  const dateStr = todayDateIST();
  const roll = String(params.roll || '').trim();
  const operation = String(params.operation || 'ADD').trim().toUpperCase();
  const sessionType = String(params.sessionType || (isSaturdayIST() ? 'SATURDAY' : 'POST_3PM')).trim().toUpperCase();
  const endTime = String(params.endTime || '05:00 PM').trim();

  if (!roll) return jsonOut({ ok: false, msg: 'Roll number is required.' });

  const allAttendance = buildAllAttendanceList(ss, dateStr);
  const trainee = findTraineeByRoll(allAttendance, roll);
  const name = trainee ? trainee.name : ('Trainee #' + roll);
  const pct = trainee ? trainee.pct : 0;

  const sheet = getOrCreateSheet(ss, 'Shop_Floor', [
    'ID', 'Date', 'Roll', 'Name', 'Attendance_Pct', 'Session_Type', 'Scheduled_End',
    'Assigned_Reason', 'Status', 'Check_In_Time', 'Check_Out_Card_Scan_Time',
    'Duration_Minutes', 'Verified', 'Manual_Override'
  ]);
  const rows = sheet.getDataRange().getValues();
  let existingRow = -1;
  for (let i = 1; i < rows.length; i++) {
    if (String(rows[i][1]).trim() === dateStr && String(rows[i][2]).trim() === roll) {
      existingRow = i + 1;
      break;
    }
  }

  if (operation === 'REMOVE') {
    if (existingRow !== -1) {
      sheet.getRange(existingRow, 9).setValue('REMOVED');
      sheet.getRange(existingRow, 14).setValue(true);
    } else {
      sheet.appendRow([
        'SF-' + dateStr + '-' + roll, dateStr, roll, name, pct, sessionType,
        endTime, 'Instructor Removed', 'REMOVED', '-', '', 0, false, true
      ]);
    }
    return jsonOut({
      ok: true,
      msg: 'Removed Roll #' + roll + ' (' + name + ') from today\'s Shop Floor list.'
    });
  }

  const checkIn = sessionType === 'SATURDAY' ? '09:30 AM' : '03:00 PM';
  const reason = pct < EXAM_THRESHOLD_PCT ? 'Under 80% + Instructor Scheduled' : 'Manual Instructor Assignment';

  if (existingRow !== -1) {
    sheet.getRange(existingRow, 5, 1, 6).setValues([[pct, sessionType, endTime, reason, 'SCHEDULED', checkIn]]);
    sheet.getRange(existingRow, 14).setValue(true);
  } else {
    sheet.appendRow([
      'SF-' + dateStr + '-' + roll, dateStr, roll, name, pct, sessionType,
      endTime, reason, 'SCHEDULED', checkIn, '', 0, false, true
    ]);
  }

  return jsonOut({
    ok: true,
    msg: 'Added Roll #' + roll + ' (' + name + ') to Shop Floor (' + sessionType + ' until ' + endTime + ').'
  });
}

function recordShopFloorCardLogoutInternal(ss, dateStr, roll, allAttendance) {
  const trainee = findTraineeByRoll(allAttendance, roll);
  const name = trainee ? trainee.name : ('Trainee #' + roll);
  const pct = trainee ? trainee.pct : 0;
  const scanTime = nowTimeIST();

  // Compute approximate duration from 3:00 PM (or 9:30 AM on Saturday)
  const startMinutes = isSaturdayIST() ? (9 * 60 + 30) : (15 * 60);
  const nowMinutes = (nowHourIST() * 60) + nowMinuteIST();
  const durationMinutes = Math.max(15, nowMinutes - startMinutes);

  const sheet = getOrCreateSheet(ss, 'Shop_Floor', [
    'ID', 'Date', 'Roll', 'Name', 'Attendance_Pct', 'Session_Type', 'Scheduled_End',
    'Assigned_Reason', 'Status', 'Check_In_Time', 'Check_Out_Card_Scan_Time',
    'Duration_Minutes', 'Verified', 'Manual_Override'
  ]);
  const rows = sheet.getDataRange().getValues();
  let existingRow = -1;
  for (let i = 1; i < rows.length; i++) {
    if (String(rows[i][1]).trim() === dateStr && String(rows[i][2]).trim() === roll) {
      existingRow = i + 1;
      break;
    }
  }

  if (existingRow !== -1) {
    sheet.getRange(existingRow, 9).setValue('COMPLETED');
    sheet.getRange(existingRow, 11).setValue(scanTime);
    sheet.getRange(existingRow, 12).setValue(durationMinutes);
  } else {
    sheet.appendRow([
      'SF-' + dateStr + '-' + roll,
      dateStr,
      roll,
      name,
      pct,
      isSaturdayIST() ? 'SATURDAY' : 'POST_3PM',
      '05:00 PM',
      'Auto (<80%)',
      'COMPLETED',
      isSaturdayIST() ? '09:30 AM' : '03:00 PM',
      scanTime,
      durationMinutes,
      false,
      false
    ]);
  }

  return {
    roll: roll,
    name: name,
    scanTime: scanTime,
    durationMinutes: durationMinutes
  };
}

function handleShopFloorCardLogout(ss, params) {
  if (String(params.key || '').trim() !== ESP_SECRET_KEY) {
    return jsonOut({ ok: false, msg: 'Unauthorized ESP key.' });
  }
  const roll = String(params.roll || '').trim();
  if (!roll) return jsonOut({ ok: false, msg: 'Roll number is required.' });

  const dateStr = todayDateIST();
  const allAttendance = buildAllAttendanceList(ss, dateStr);
  const res = recordShopFloorCardLogoutInternal(ss, dateStr, roll, allAttendance);

  PropertiesService.getScriptProperties().setProperty('LAST_SCANNER_SYNC', dateStr + ' ' + res.scanTime);

  return jsonOut({
    ok: true,
    mode: 'SHOP_FLOOR_OUT',
    roll: res.roll,
    name: res.name,
    time: res.scanTime,
    checkOutCardScanTime: res.scanTime,
    durationMinutes: res.durationMinutes,
    msg: 'Shop Floor ID-card logout recorded for Roll #' + res.roll + ' at ' + res.scanTime + ' (' + res.durationMinutes + ' mins).'
  });
}

function handleVerifyShopFloorLogout(ss, params) {
  const dateStr = String(params.date || todayDateIST()).trim();
  const roll = String(params.roll || '').trim();
  if (!roll) return jsonOut({ ok: false, msg: 'Roll number is required.' });

  const sheet = getOrCreateSheet(ss, 'Shop_Floor', [
    'ID', 'Date', 'Roll', 'Name', 'Attendance_Pct', 'Session_Type', 'Scheduled_End',
    'Assigned_Reason', 'Status', 'Check_In_Time', 'Check_Out_Card_Scan_Time',
    'Duration_Minutes', 'Verified', 'Manual_Override'
  ]);
  const rows = sheet.getDataRange().getValues();
  for (let i = 1; i < rows.length; i++) {
    if (String(rows[i][1]).trim() === dateStr && String(rows[i][2]).trim() === roll) {
      sheet.getRange(i + 1, 9).setValue('VERIFIED');
      sheet.getRange(i + 1, 13).setValue(true);
      return jsonOut({
        ok: true,
        msg: 'Shop Floor logout verified by Trade Instructor for Roll #' + roll + '.'
      });
    }
  }
  return jsonOut({ ok: false, msg: 'No Shop Floor record found for Roll #' + roll + ' on ' + dateStr + '.' });
}

// ============================================================================
// 4. ESP8266 RFID TAP HANDLER (WITH EARLY CLASS & SHOP FLOOR RELEASE OVERRIDES)
// ============================================================================

function handleEspTap(ss, params) {
  if (String(params.key || '').trim() !== ESP_SECRET_KEY) {
    return jsonOut({ ok: false, msg: 'Invalid ESP key.' });
  }
  const roll = String(params.roll || '').trim();
  if (!roll) return jsonOut({ ok: false, msg: 'Missing roll number.' });

  const dateStr = todayDateIST();
  const timeStr = nowTimeIST();
  const hourNow = nowHourIST();

  const allAttendance = buildAllAttendanceList(ss, dateStr);
  const trainee = findTraineeByRoll(allAttendance, roll);
  const name = trainee ? trainee.name : ('Trainee #' + roll);

  const overrides = getScanOverridesState(ss);
  const shopFloorList = getShopFloorList(ss, dateStr, allAttendance);
  const onShopFloor = shopFloorList.some(function (sf) {
    return String(sf.roll) === roll && sf.status !== 'REMOVED';
  });

  PropertiesService.getScriptProperties().setProperty('LAST_SCANNER_SYNC', dateStr + ' ' + timeStr);

  // OVERRIDE 1: Instructor clicked "Release Shop Floor Now" OR normal after-hours Shop Floor logout
  if (onShopFloor && (overrides.shopFloorReleaseNowEnabled || isSaturdayIST() || hourNow >= CLASS_END_HOUR_IST)) {
    // Only treat as Shop Floor logout if trainee already tapped IN (or it's Saturday / Shop Floor Release Now is active)
    if (overrides.shopFloorReleaseNowEnabled || isSaturdayIST() || (trainee && trainee.inT !== '-')) {
      const sfRes = recordShopFloorCardLogoutInternal(ss, dateStr, roll, allAttendance);
      return jsonOut({
        ok: true,
        mode: 'SHOP_FLOOR_OUT',
        roll: roll,
        name: name,
        time: sfRes.scanTime,
        checkOutCardScanTime: sfRes.scanTime,
        durationMinutes: sfRes.durationMinutes,
        msg: (overrides.shopFloorReleaseNowEnabled ? '[EARLY RELEASE] ' : '') +
          'Shop Floor ID-card logout recorded for ' + name + ' at ' + sfRes.scanTime + '.'
      });
    }
  }

  // NORMAL / EARLY CLASS LOGOUT RECORDING IN Attendance_Log
  const logSheet = getOrCreateSheet(ss, 'Attendance_Log', [
    'Date', 'Roll', 'Name', 'Status', 'In_Time', 'Out_Time', 'Notes'
  ]);
  const rows = logSheet.getDataRange().getValues();
  let existingRow = -1;
  for (let i = 1; i < rows.length; i++) {
    if (String(rows[i][0]).trim() === dateStr && String(rows[i][1]).trim() === roll) {
      existingRow = i + 1;
      break;
    }
  }

  if (existingRow === -1) {
    // First tap of the day -> IN (respect active Late Grace for today)
    const nowMinutes = (hourNow * 60) + nowMinuteIST();
    const defaultCutoffMinutes = 9 * 60 + 15; // 09:15 AM standard cutoff
    const graceMode = trainee ? String(trainee.lateGraceMode || '').toUpperCase() : '';
    const graceCutoffMins = trainee ? parseTimeToMinutes(trainee.lateCutoff) : -1;

    let status = (nowMinutes > defaultCutoffMinutes) ? 'LATE' : 'PRESENT';
    let tapNote = 'RFID Tap IN';
    if (graceMode === 'EXEMPT_TODAY') {
      status = 'PRESENT';
      tapNote = 'RFID Tap IN (Late Exempt)';
    } else if (graceMode === 'CUSTOM_CUTOFF' && graceCutoffMins >= 0 && nowMinutes <= graceCutoffMins) {
      status = 'PRESENT';
      tapNote = 'RFID Tap IN (Late Grace ' + trainee.lateCutoff + ')';
    }

    logSheet.appendRow([dateStr, roll, name, status, timeStr, '-', tapNote]);
    incrementTraineeTotalsIfNeeded(ss, trainee, status);
    return jsonOut({
      ok: true,
      mode: status,
      roll: roll,
      name: name,
      time: timeStr,
      msg: name + ' marked ' + status + ' (IN at ' + timeStr + ').'
    });
  } else {
    // Second tap -> OUT
    // OVERRIDE 2: If "Allow Early Class Logout Now" is ON, allow immediate OUT even before 3:00 PM
    if (!overrides.earlyClassLogoutEnabled && hourNow < CLASS_END_HOUR_IST) {
      return jsonOut({
        ok: false,
        mode: 'BLOCKED_EARLY_OUT',
        roll: roll,
        name: name,
        time: timeStr,
        msg: 'Normal logout opens at 03:00 PM. Instructor must enable "Allow Early Class Logout Now" for early dismissal.'
      });
    }

    const note = overrides.earlyClassLogoutEnabled ? 'Early Class Logout Override' : 'RFID Tap OUT';
    logSheet.getRange(existingRow, 6).setValue(timeStr);
    logSheet.getRange(existingRow, 7).setValue(note);

    return jsonOut({
      ok: true,
      mode: 'OUT',
      roll: roll,
      name: name,
      time: timeStr,
      msg: (overrides.earlyClassLogoutEnabled ? '[EARLY DISMISSAL] ' : '') +
        name + ' logged OUT normally at ' + timeStr + '.'
    });
  }
}

function incrementTraineeTotalsIfNeeded(ss, trainee, newStatus) {
  if (!trainee || !trainee.rowIndex) return;
  const sheet = ss.getSheetByName('Trainees');
  if (!sheet) return;
  const p = Number(sheet.getRange(trainee.rowIndex, 5).getValue()) || 0;
  const h = Number(sheet.getRange(trainee.rowIndex, 6).getValue()) || 0;
  const t = Number(sheet.getRange(trainee.rowIndex, 7).getValue()) || 0;
  const streak = Number(sheet.getRange(trainee.rowIndex, 8).getValue()) || 0;

  if (newStatus === 'HALF-DAY') {
    sheet.getRange(trainee.rowIndex, 6).setValue(h + 1);
  } else if (newStatus === 'PRESENT' || newStatus === 'LATE' || newStatus === 'ON-DUTY') {
    sheet.getRange(trainee.rowIndex, 5).setValue(p + 1);
    sheet.getRange(trainee.rowIndex, 8).setValue(streak + 1);
  }
  sheet.getRange(trainee.rowIndex, 7).setValue(Math.max(t, p + h + 1));
}

// ============================================================================
// 5. DASHBOARD DATA ENDPOINTS (TRAINEE, PARENT, STAFF / PRINCIPAL)
// ============================================================================

function handleTraineeData(ss, params) {
  const roll = String(params.roll || '').trim();
  const pin = String(params.pin || '').trim();
  const dateStr = todayDateIST();

  const allAttendance = buildAllAttendanceList(ss, dateStr);
  const trainee = findTraineeByRoll(allAttendance, roll);
  if (!trainee) {
    return jsonOut({ ok: false, msg: 'Roll #' + roll + ' not found in Trainees sheet.' });
  }
  if (trainee.traineePin && trainee.traineePin !== pin) {
    return jsonOut({ ok: false, msg: 'Invalid Trainee PIN for Roll #' + roll + '.' });
  }

  const closeState = getDailyCloseState(ss, dateStr);
  const holidays = getHolidaysList(ss);
  const todayHoliday = getHolidayForDate(ss, dateStr);
  const notices = getNoticesList(ss);
  const pinnedBroadcast = getPinnedBroadcast(notices);
  const shopFloorList = getShopFloorList(ss, dateStr, allAttendance);
  const myShopFloor = shopFloorList.filter(function (s) { return String(s.roll) === roll; })[0] || null;
  const leaves = getLeavesForRoll(ss, roll);

  return jsonOut({
    ok: true,
    roll: trainee.roll,
    name: trainee.name,
    attendance: {
      todayStatus: trainee.todayStatus,
      inT: trainee.inT,
      outT: trainee.outT,
      present: trainee.present,
      half: trainee.half,
      total: trainee.total,
      pct: trainee.pct,
      streakDays: trainee.streakDays
    },
    leaves: leaves,
    attendanceClosed: closeState.attendanceClosed,
    attendanceDate: closeState.attendanceDate,
    closedAt: closeState.closedAt,
    todayHoliday: todayHoliday,
    holidays: holidays,
    notices: notices,
    pinnedBroadcast: pinnedBroadcast,
    myShopFloor: myShopFloor,
    scanOverrides: getScanOverridesState(ss),
    projectGroupsEnabled: isProjectGroupsEnabled(),
    projectGroups: getProjectGroupsList(ss),
    monthlyMarks: getMonthlyMarksList(ss, roll, true),
    myWorkMarkQueries: getWorkMarkQueriesList(ss, roll),
    myRunningLateAlerts: getRunningLateAlertsList(ss, roll),
    myAcademicSubmission: getAcademicSubmissionsList(ss, roll)[0] || null
  });
}

function handleParentData(ss, params) {
  const roll = String(params.roll || '').trim();
  const pin = String(params.pin || '').trim();
  const dateStr = todayDateIST();

  const allAttendance = buildAllAttendanceList(ss, dateStr);
  const trainee = findTraineeByRoll(allAttendance, roll);
  if (!trainee) {
    return jsonOut({ ok: false, msg: 'Roll #' + roll + ' not found.' });
  }
  if (trainee.parentPin && trainee.parentPin !== pin && trainee.traineePin !== pin) {
    return jsonOut({ ok: false, msg: 'Invalid Parent PIN for Roll #' + roll + '.' });
  }

  const closeState = getDailyCloseState(ss, dateStr);
  const holidays = getHolidaysList(ss);
  const todayHoliday = getHolidayForDate(ss, dateStr);
  const notices = getNoticesList(ss);
  const pinnedBroadcast = getPinnedBroadcast(notices);
  const shopFloorList = getShopFloorList(ss, dateStr, allAttendance);
  const wardShopFloor = shopFloorList.filter(function (s) { return String(s.roll) === roll; })[0] || null;

  return jsonOut({
    ok: true,
    roll: trainee.roll,
    name: trainee.name,
    studentName: trainee.name,
    attendance: {
      todayStatus: trainee.todayStatus,
      inT: trainee.inT,
      outT: trainee.outT,
      present: trainee.present,
      half: trainee.half,
      total: trainee.total,
      pct: trainee.pct,
      streakDays: trainee.streakDays
    },
    attendanceClosed: closeState.attendanceClosed,
    attendanceDate: closeState.attendanceDate,
    closedAt: closeState.closedAt,
    todayHoliday: todayHoliday,
    holidays: holidays,
    notices: notices,
    pinnedBroadcast: pinnedBroadcast,
    wardShopFloor: wardShopFloor,
    parentAlerts: getParentAlertsList(ss, roll),
    monthlyMarks: getMonthlyMarksList(ss, roll, true)
  });
}

function handleStaffData(ss, params) {
  const role = getStaffRoleForPin(params.pin);
  if (!role) {
    return jsonOut({ ok: false, msg: 'Invalid Staff PIN.' });
  }

  const dateStr = todayDateIST();
  const allAttendance = buildAllAttendanceList(ss, dateStr);
  const closeState = getDailyCloseState(ss, dateStr);
  const holidays = getHolidaysList(ss);
  const todayHoliday = getHolidayForDate(ss, dateStr);
  const notices = getNoticesList(ss);
  const pinnedBroadcast = getPinnedBroadcast(notices);
  const shopFloorList = getShopFloorList(ss, dateStr, allAttendance);
  const pendingLeaves = getPendingLeavesForRole(ss, role);
  const lastSync = PropertiesService.getScriptProperties().getProperty('LAST_SCANNER_SYNC') || '';

  return jsonOut({
    ok: true,
    role: role,
    allAttendance: allAttendance.map(function (t) {
      return {
        roll: t.roll,
        name: t.name,
        todayStatus: t.todayStatus,
        inT: t.inT,
        outT: t.outT,
        present: t.present,
        half: t.half,
        total: t.total,
        pct: t.pct,
        lateGraceMode: t.lateGraceMode || '',
        lateCutoff: t.lateCutoff || '',
        lateGraceNote: t.lateGraceNote || ''
      };
    }),
    pendingLeaves: pendingLeaves,
    parentAlerts: getParentAlertsList(ss, ''),
    attendanceClosed: closeState.attendanceClosed,
    attendanceDate: closeState.attendanceDate,
    closedAt: closeState.closedAt,
    todayHoliday: todayHoliday,
    holidays: holidays,
    notices: notices,
    pinnedBroadcast: pinnedBroadcast,
    shopFloorList: shopFloorList,
    scanOverrides: getScanOverridesState(ss),
    toolCribLogs: getToolCribLogsList(ss),
    todayDutySignOff: getTodayDutySignOff(ss, dateStr),
    lastScannerSync: lastSync,
    projectGroupsEnabled: isProjectGroupsEnabled(),
    projectGroups: getProjectGroupsList(ss),
    monthlyMarks: getMonthlyMarksList(ss, '', false),
    workMarkQueries: getWorkMarkQueriesList(ss, ''),
    runningLateAlerts: getRunningLateAlertsList(ss, ''),
    academicSubmissions: getAcademicSubmissionsList(ss, '')
  });
}

// ============================================================================
// 5B. LATE GRACE QUICK-MARK SYSTEM (Late_Grace SHEET + AUTO PRESENT UPGRADE)
// ============================================================================

/**
 * Parses time strings like "09:30", "09:18 AM", "9:25 am", or "13:05" into minutes since midnight.
 * Returns -1 if the string cannot be parsed.
 */
function parseTimeToMinutes(raw) {
  const s = String(raw || '').trim().toUpperCase();
  if (!s || s === '-') return -1;
  const match = s.match(/^(\d{1,2}):(\d{2})(?:\s*(AM|PM))?$/);
  if (!match) return -1;
  let hh = parseInt(match[1], 10);
  const mm = parseInt(match[2], 10);
  const ampm = match[3] || '';
  if (isNaN(hh) || isNaN(mm) || mm < 0 || mm > 59) return -1;
  if (ampm === 'PM' && hh < 12) hh += 12;
  if (ampm === 'AM' && hh === 12) hh = 0;
  if (hh < 0 || hh > 23) return -1;
  return (hh * 60) + mm;
}

/**
 * Returns a map keyed by roll for today's active Late Grace entries.
 */
function getLateGraceMapForDate(ss, dateStr) {
  const sheet = getOrCreateSheet(ss, 'Late_Grace', [
    'Date', 'Roll', 'Name', 'Mode', 'Cutoff', 'Note', 'Updated_At', 'Updated_By'
  ]);
  const rows = sheet.getDataRange().getValues();
  const map = {};
  for (let i = 1; i < rows.length; i++) {
    const rDate = String(rows[i][0] || '').trim();
    const rRoll = String(rows[i][1] || '').trim();
    if (!rRoll || rDate !== dateStr) continue;
    const mode = String(rows[i][3] || '').trim().toUpperCase();
    if (mode === 'CUSTOM_CUTOFF' || mode === 'EXEMPT_TODAY') {
      map[rRoll] = {
        mode: mode,
        cutoff: String(rows[i][4] || '').trim(),
        note: String(rows[i][5] || '').trim()
      };
    } else {
      delete map[rRoll];
    }
  }
  return map;
}

/**
 * Action: set_late_grace&pin=<PIN>&roll=<ROLL>&mode=<CUSTOM_CUTOFF|EXEMPT_TODAY|CLEAR>&cutoff=<HH:MM>&note=<NOTE>
 * Saves today's (dd-MM-yyyy IST) Late Grace status and immediately converts today's LATE / HALF entry
 * to PRESENT if the trainee's IN scan time is within the grace cutoff (or if EXEMPT_TODAY).
 */
function handleSetLateGrace(ss, params, role) {
  const dateStr = String(params.date || todayDateIST()).trim();
  const roll = String(params.roll || '').trim();
  const mode = String(params.mode || 'CLEAR').trim().toUpperCase();
  const cutoff = (mode === 'CUSTOM_CUTOFF')
    ? (String(params.cutoff || '09:30').trim() || '09:30')
    : '';
  const note = String(params.note || '').trim();
  const updatedAt = nowTimeIST();

  if (!roll) return jsonOut({ ok: false, msg: 'Roll number is required.' });

  const allAttendance = buildAllAttendanceList(ss, dateStr);
  const trainee = findTraineeByRoll(allAttendance, roll);
  const name = trainee ? trainee.name : ('Trainee #' + roll);

  const graceSheet = getOrCreateSheet(ss, 'Late_Grace', [
    'Date', 'Roll', 'Name', 'Mode', 'Cutoff', 'Note', 'Updated_At', 'Updated_By'
  ]);
  const graceRows = graceSheet.getDataRange().getValues();
  let existingGraceRow = -1;
  for (let i = 1; i < graceRows.length; i++) {
    if (String(graceRows[i][0]).trim() === dateStr && String(graceRows[i][1]).trim() === roll) {
      existingGraceRow = i + 1;
      break;
    }
  }

  const storedMode = (mode === 'CUSTOM_CUTOFF' || mode === 'EXEMPT_TODAY') ? mode : '';
  if (existingGraceRow !== -1) {
    graceSheet.getRange(existingGraceRow, 3, 1, 6).setValues([[
      name, storedMode, cutoff, note, updatedAt, role
    ]]);
  } else {
    graceSheet.appendRow([dateStr, roll, name, storedMode, cutoff, note, updatedAt, role]);
  }

  // Immediately convert today's LATE / HALF entry in Attendance_Log to PRESENT if eligible
  let convertedToPresent = false;
  if (storedMode === 'EXEMPT_TODAY' || storedMode === 'CUSTOM_CUTOFF') {
    const logSheet = getOrCreateSheet(ss, 'Attendance_Log', [
      'Date', 'Roll', 'Name', 'Status', 'In_Time', 'Out_Time', 'Notes'
    ]);
    const logRows = logSheet.getDataRange().getValues();
    for (let i = 1; i < logRows.length; i++) {
      if (String(logRows[i][0]).trim() === dateStr && String(logRows[i][1]).trim() === roll) {
        const curStatus = String(logRows[i][3] || '').trim().toUpperCase();
        const inTimeStr = String(logRows[i][4] || '').trim();
        const isLateOrHalf = (curStatus === 'LATE' || curStatus.indexOf('HALF') !== -1);

        if (isLateOrHalf) {
          let eligible = (storedMode === 'EXEMPT_TODAY');
          if (!eligible && storedMode === 'CUSTOM_CUTOFF') {
            const inMins = parseTimeToMinutes(inTimeStr);
            const cutoffMins = parseTimeToMinutes(cutoff);
            if (inMins >= 0 && cutoffMins >= 0 && inMins <= cutoffMins) {
              eligible = true;
            }
          }

          if (eligible) {
            const reasonTag = storedMode === 'EXEMPT_TODAY'
              ? 'Late Grace: Exempt Today'
              : ('Late Grace: Cutoff ' + cutoff);
            logSheet.getRange(i + 1, 4).setValue('PRESENT');
            logSheet.getRange(i + 1, 7).setValue(reasonTag + ' (' + role + ')');
            convertedToPresent = true;

            // If converting from HALF-DAY to PRESENT, reconcile Trainees sheet counters
            if (curStatus.indexOf('HALF') !== -1 && trainee && trainee.rowIndex) {
              const tSheet = ss.getSheetByName('Trainees');
              if (tSheet) {
                const curP = Number(tSheet.getRange(trainee.rowIndex, 5).getValue()) || 0;
                const curH = Number(tSheet.getRange(trainee.rowIndex, 6).getValue()) || 0;
                tSheet.getRange(trainee.rowIndex, 5).setValue(curP + 1);
                tSheet.getRange(trainee.rowIndex, 6).setValue(Math.max(0, curH - 1));
              }
            }
          }
        }
        break;
      }
    }
  }

  let msg = '';
  if (storedMode === 'CUSTOM_CUTOFF') {
    msg = 'Roll #' + roll + ' (' + name + '): ' + cutoff + ' Late Grace active for ' + dateStr +
      (convertedToPresent ? ' — converted to PRESENT!' : '.');
  } else if (storedMode === 'EXEMPT_TODAY') {
    msg = 'Roll #' + roll + ' (' + name + '): Exempt from late penalty today' +
      (convertedToPresent ? ' — converted to PRESENT!' : '.');
  } else {
    msg = 'Roll #' + roll + ' (' + name + '): Late Grace cleared for ' + dateStr + '.';
  }

  return jsonOut({
    ok: true,
    roll: roll,
    date: dateStr,
    lateGraceMode: storedMode,
    lateCutoff: cutoff,
    convertedToPresent: convertedToPresent,
    msg: msg
  });
}

// ============================================================================
// 6. BULK MORNING ROLL CALL, QUICK EXCEPTIONS, DUTY SIGN-OFF & TOOL CRIB
// ============================================================================

function handleBulkRollCall(ss, params) {
  const dateStr = String(params.date || todayDateIST()).trim();
  const status = String(params.status || 'PRESENT').trim().toUpperCase();
  const timeStr = nowTimeIST();

  const allAttendance = buildAllAttendanceList(ss, dateStr);
  const logSheet = getOrCreateSheet(ss, 'Attendance_Log', [
    'Date', 'Roll', 'Name', 'Status', 'In_Time', 'Out_Time', 'Notes'
  ]);
  const rows = logSheet.getDataRange().getValues();
  const existingMap = {};
  for (let i = 1; i < rows.length; i++) {
    if (String(rows[i][0]).trim() === dateStr) {
      existingMap[String(rows[i][1]).trim()] = i + 1;
    }
  }

  let markedCount = 0;
  allAttendance.forEach(function (t) {
    if (!existingMap[t.roll]) {
      logSheet.appendRow([dateStr, t.roll, t.name, status, timeStr, '-', 'Bulk Morning Roll Call']);
      incrementTraineeTotalsIfNeeded(ss, t, status);
      markedCount++;
    }
  });

  return jsonOut({
    ok: true,
    markedCount: markedCount,
    msg: 'Bulk Roll Call applied: ' + markedCount + ' trainees marked ' + status + ' for ' + dateStr + '.'
  });
}

function handleMarkException(ss, params) {
  const dateStr = String(params.date || todayDateIST()).trim();
  const roll = String(params.roll || '').trim();
  const exceptionType = String(params.exceptionType || 'PRESENT').trim().toUpperCase();
  const timeStr = nowTimeIST();

  if (!roll) return jsonOut({ ok: false, msg: 'Roll number is required.' });

  const allAttendance = buildAllAttendanceList(ss, dateStr);
  const trainee = findTraineeByRoll(allAttendance, roll);
  const name = trainee ? trainee.name : ('Trainee #' + roll);

  const logSheet = getOrCreateSheet(ss, 'Attendance_Log', [
    'Date', 'Roll', 'Name', 'Status', 'In_Time', 'Out_Time', 'Notes'
  ]);
  const rows = logSheet.getDataRange().getValues();
  let existingRow = -1;
  for (let i = 1; i < rows.length; i++) {
    if (String(rows[i][0]).trim() === dateStr && String(rows[i][1]).trim() === roll) {
      existingRow = i + 1;
      break;
    }
  }

  if (existingRow !== -1) {
    logSheet.getRange(existingRow, 4).setValue(exceptionType);
    logSheet.getRange(existingRow, 7).setValue('Instructor Exception: ' + exceptionType);
  } else {
    const inTime = exceptionType === 'ABSENT' ? '-' : timeStr;
    logSheet.appendRow([dateStr, roll, name, exceptionType, inTime, '-', 'Instructor Exception']);
    if (exceptionType !== 'ABSENT') {
      incrementTraineeTotalsIfNeeded(ss, trainee, exceptionType);
    }
  }

  return jsonOut({
    ok: true,
    msg: 'Updated Roll #' + roll + ' (' + name + ') to ' + exceptionType + '.'
  });
}

function getTodayDutySignOff(ss, dateStr) {
  const sheet = getOrCreateSheet(ss, 'Duty_Rotation', [
    'Date', 'Weekday', 'Pair_Label', 'Signed_Off', 'Signed_At', 'Note'
  ]);
  const rows = sheet.getDataRange().getValues();
  for (let i = rows.length - 1; i >= 1; i--) {
    if (String(rows[i][0]).trim() === dateStr) {
      return {
        date: dateStr,
        pairLabel: String(rows[i][2] || '').trim(),
        signedOff: String(rows[i][3]).toLowerCase() === 'true',
        signedAt: String(rows[i][4] || '').trim(),
        note: String(rows[i][5] || '').trim()
      };
    }
  }
  return null;
}

function handleSignOffDuty(ss, params) {
  const dateStr = String(params.date || todayDateIST()).trim();
  const pairLabel = String(params.pairLabel || '').trim();
  const note = String(params.note || 'Verified').trim();
  const signedAt = nowTimeIST();
  const weekday = todayWeekdayNameIST();

  if (isWeekendIST()) {
    return jsonOut({
      ok: false,
      msg: 'Weekend (' + weekday + ') — Weekday Mon–Fri Eggs/Milk duty rotation is paused on Saturday & Sunday.'
    });
  }

  const sheet = getOrCreateSheet(ss, 'Duty_Rotation', [
    'Date', 'Weekday', 'Pair_Label', 'Signed_Off', 'Signed_At', 'Note'
  ]);
  sheet.appendRow([dateStr, weekday, pairLabel, true, signedAt, note]);

  return jsonOut({
    ok: true,
    signedAt: signedAt,
    msg: 'Weekday duty (' + pairLabel + ') signed off at ' + signedAt + '.'
  });
}

function getToolCribLogsList(ss) {
  const sheet = getOrCreateSheet(ss, 'Tool_Crib_Log', [
    'ID', 'Date', 'Time', 'Item_Name', 'Condition_Status', 'Notes', 'Reported_By'
  ]);
  const rows = sheet.getDataRange().getValues();
  const out = [];
  for (let i = rows.length - 1; i >= 1 && out.length < 40; i--) {
    if (!rows[i][3]) continue;
    out.push({
      id: String(rows[i][0] || ('TC-' + i)),
      date: String(rows[i][1] || ''),
      time: String(rows[i][2] || ''),
      itemName: String(rows[i][3] || ''),
      conditionStatus: String(rows[i][4] || 'DAMAGED'),
      notes: String(rows[i][5] || ''),
      reportedBy: String(rows[i][6] || 'Trade Instructor')
    });
  }
  return out;
}

function handleLogToolCribIssue(ss, params, role) {
  const itemName = String(params.itemName || '').trim();
  const conditionStatus = String(params.conditionStatus || 'DAMAGED').trim().toUpperCase();
  const notes = String(params.notes || '').trim();
  if (!itemName) return jsonOut({ ok: false, msg: 'Equipment / tool name is required.' });

  const id = 'TC-' + Date.now();
  const dateStr = todayDateIST();
  const timeStr = nowTimeIST();

  const sheet = getOrCreateSheet(ss, 'Tool_Crib_Log', [
    'ID', 'Date', 'Time', 'Item_Name', 'Condition_Status', 'Notes', 'Reported_By'
  ]);
  sheet.appendRow([id, dateStr, timeStr, itemName, conditionStatus, notes, role]);

  return jsonOut({
    ok: true,
    id: id,
    date: dateStr,
    time: timeStr,
    reportedBy: role,
    msg: 'Logged ' + itemName + ' (' + conditionStatus + ') in Tool Crib ledger.'
  });
}

// ============================================================================
// 7. CONFIRM & CLOSE, HOLIDAYS, NOTICES (WITH PINNED BROADCAST) & PARENT ALERTS
// ============================================================================

function getDailyCloseState(ss, dateStr) {
  const sheet = getOrCreateSheet(ss, 'DailyCloseLog', [
    'Date', 'Closed', 'ClosedAt', 'ClosedBy', 'AbsentCount', 'HalfDayCount'
  ]);
  const rows = sheet.getDataRange().getValues();
  for (let i = rows.length - 1; i >= 1; i--) {
    if (String(rows[i][0]).trim() === dateStr) {
      return {
        attendanceClosed: String(rows[i][1]).toLowerCase() === 'true',
        attendanceDate: dateStr,
        closedAt: String(rows[i][2] || '').trim()
      };
    }
  }
  return { attendanceClosed: false, attendanceDate: dateStr, closedAt: '' };
}

function handleConfirmCloseAttendance(ss, params) {
  const dateStr = String(params.date || todayDateIST()).trim();
  const closedAt = nowTimeIST();
  const allAttendance = buildAllAttendanceList(ss, dateStr);
  const todayHoliday = getHolidayForDate(ss, dateStr);

  let absentCount = 0;
  let halfDayCount = 0;
  allAttendance.forEach(function (r) {
    const st = String(r.todayStatus || '').toUpperCase();
    if (st === 'ABSENT' || st === '') absentCount++;
    else if (st.indexOf('HALF') !== -1) halfDayCount++;
  });

  const sheet = getOrCreateSheet(ss, 'DailyCloseLog', [
    'Date', 'Closed', 'ClosedAt', 'ClosedBy', 'AbsentCount', 'HalfDayCount'
  ]);
  const rows = sheet.getDataRange().getValues();
  let updated = false;
  for (let i = 1; i < rows.length; i++) {
    if (String(rows[i][0]).trim() === dateStr) {
      sheet.getRange(i + 1, 2, 1, 5).setValues([[true, closedAt, 'Trade Instructor', absentCount, halfDayCount]]);
      updated = true;
      break;
    }
  }
  if (!updated) {
    sheet.appendRow([dateStr, true, closedAt, 'Trade Instructor', absentCount, halfDayCount]);
  }

  const msg = todayHoliday
    ? 'Attendance closed for ' + dateStr + ' (Declared Holiday — ABSENT alerts skipped).'
    : 'Today\'s attendance confirmed & closed at ' + closedAt + '. Parent alerts active for ' + absentCount + ' Absent & ' + halfDayCount + ' Half-Day trainees.';

  return jsonOut({
    ok: true,
    attendanceClosed: true,
    date: dateStr,
    closedAt: closedAt,
    absentCount: absentCount,
    halfDayCount: halfDayCount,
    msg: msg
  });
}

function getHolidaysList(ss) {
  const sheet = getOrCreateSheet(ss, 'Holidays', ['ID', 'Date', 'Reason', 'PostedBy', 'PostedAt']);
  const rows = sheet.getDataRange().getValues();
  const out = [];
  for (let i = rows.length - 1; i >= 1; i--) {
    if (!rows[i][1]) continue;
    out.push({
      id: String(rows[i][0] || ('HOL-' + i)),
      date: String(rows[i][1]).trim(),
      reason: String(rows[i][2] || '').trim(),
      postedBy: String(rows[i][3] || 'Trade Instructor'),
      postedAt: String(rows[i][4] || '').trim()
    });
  }
  return out;
}

function getHolidayForDate(ss, dateStr) {
  const list = getHolidaysList(ss);
  for (let i = 0; i < list.length; i++) {
    if (list[i].date === dateStr) return list[i];
  }
  return null;
}

function handlePostHoliday(ss, params, role) {
  const dateStr = String(params.date || todayDateIST()).trim();
  const reason = String(params.reason || '').trim();
  if (!reason) return jsonOut({ ok: false, msg: 'Please provide a holiday reason.' });

  const sheet = getOrCreateSheet(ss, 'Holidays', ['ID', 'Date', 'Reason', 'PostedBy', 'PostedAt']);
  const id = 'HOL-' + Date.now();
  const postedAt = nowTimeIST();
  sheet.appendRow([id, dateStr, reason, role, postedAt]);

  return jsonOut({
    ok: true,
    holiday: { id: id, date: dateStr, reason: reason, postedBy: role, postedAt: postedAt },
    msg: 'Holiday published for ' + dateStr + '. Absent notifications suppressed on this date.'
  });
}

function getNoticesList(ss) {
  const sheet = getOrCreateSheet(ss, 'Notices', ['ID', 'Date', 'Title', 'Category', 'Body', 'PostedBy', 'Pinned']);
  const rows = sheet.getDataRange().getValues();
  const out = [];
  for (let i = rows.length - 1; i >= 1; i--) {
    if (!rows[i][2] && !rows[i][4]) continue;
    out.push({
      id: String(rows[i][0] || ('NTC-' + i)),
      date: formatCellDateTimeIST(rows[i][1]) || todayDateIST(),
      title: String(rows[i][2] || 'Trade Notice').trim(),
      category: String(rows[i][3] || 'General').trim(),
      body: String(rows[i][4] || '').trim(),
      postedBy: String(rows[i][5] || 'Govt. ITI Ranni').trim(),
      isPinned: String(rows[i][6]).toLowerCase() === 'true'
    });
  }
  return out;
}

function getPinnedBroadcast(notices) {
  for (let i = 0; i < notices.length; i++) {
    if (notices[i].isPinned) return notices[i];
  }
  return null;
}

function handlePostNotice(ss, params, role) {
  const title = String(params.title || '').trim();
  const category = String(params.category || 'General').trim();
  const body = String(params.body || '').trim();
  const dateStr = String(params.date || todayDateIST()).trim();
  const pinned = String(params.pinned || 'false').toLowerCase() === 'true';
  const postedBy = String(params.postedBy || role || 'Trade Instructor').trim();

  if (!title || !body) return jsonOut({ ok: false, msg: 'Notice title and body are required.' });

  const sheet = getOrCreateSheet(ss, 'Notices', ['ID', 'Date', 'Title', 'Category', 'Body', 'PostedBy', 'Pinned']);
  const id = 'NTC-' + Date.now();
  sheet.appendRow([id, dateStr, title, category, body, postedBy, pinned]);

  return jsonOut({
    ok: true,
    notice: {
      id: id,
      date: dateStr,
      title: title,
      category: category,
      body: body,
      postedBy: postedBy,
      isPinned: pinned
    },
    msg: pinned ? 'Pinned Principal Broadcast published to all dashboards.' : 'Notice published to students & parents.'
  });
}

function getParentAlertsList(ss, rollFilter) {
  const sheet = getOrCreateSheet(ss, 'Parent_Alerts', ['ID', 'Date', 'Time', 'Roll', 'StudentName', 'AlertType', 'Message']);
  const rows = sheet.getDataRange().getValues();
  const out = [];
  for (let i = rows.length - 1; i >= 1 && out.length < 50; i--) {
    const rRoll = String(rows[i][3] || '').trim();
    if (rollFilter && rRoll !== String(rollFilter).trim()) continue;
    out.push({
      id: String(rows[i][0] || ('PA-' + i)),
      date: formatCellDateIST(rows[i][1], '-'),
      time: formatCellTimeIST(rows[i][2]),
      roll: rRoll,
      studentName: String(rows[i][4] || ('Roll #' + rRoll)).trim(),
      alertType: String(rows[i][5] || 'LEAVE_REASON').trim(),
      message: String(rows[i][6] || '').trim()
    });
  }
  return out;
}

function handleParentSubmitAlert(ss, params) {
  const roll = String(params.roll || '').trim();
  const alertType = String(params.alertType || 'LEAVE_REASON').trim().toUpperCase();
  const message = String(params.message || '').trim();
  if (!roll || !message) return jsonOut({ ok: false, msg: 'Roll number and message are required.' });

  const allAttendance = buildAllAttendanceList(ss, todayDateIST());
  const trainee = findTraineeByRoll(allAttendance, roll);
  const studentName = trainee ? trainee.name : ('Roll #' + roll);

  const sheet = getOrCreateSheet(ss, 'Parent_Alerts', ['ID', 'Date', 'Time', 'Roll', 'StudentName', 'AlertType', 'Message']);
  const id = 'PA-' + Date.now();
  sheet.appendRow([id, todayDateIST(), nowTimeIST(), roll, studentName, alertType, message]);

  return jsonOut({
    ok: true,
    alertId: id,
    msg: 'Alert sent to Trade Instructor for ' + studentName + '.'
  });
}

function handleSendInstructorParentAlert(ss, params) {
  const roll = String(params.roll || '').trim();
  const alertType = String(params.alertType || 'LOW_ATTENDANCE_WARNING').trim().toUpperCase();
  const message = String(params.message || '').trim();
  if (!roll || !message) return jsonOut({ ok: false, msg: 'Roll number and warning message are required.' });

  const allAttendance = buildAllAttendanceList(ss, todayDateIST());
  const trainee = findTraineeByRoll(allAttendance, roll);
  const studentName = trainee ? trainee.name : ('Roll #' + roll);

  const sheet = getOrCreateSheet(ss, 'Parent_Alerts', ['ID', 'Date', 'Time', 'Roll', 'StudentName', 'AlertType', 'Message']);
  const id = 'PA-INST-' + Date.now();
  sheet.appendRow([id, todayDateIST(), nowTimeIST(), roll, studentName, alertType, message]);

  return jsonOut({
    ok: true,
    alertId: id,
    msg: 'Instant Parent Alert dispatched for Roll #' + roll + ' (' + studentName + ').'
  });
}

// ============================================================================
// 8. LEAVE / OUT-PASS WORKFLOW, PROJECT GROUPS & REGISTER PDF EXPORT
// ============================================================================

function getLeavesForRoll(ss, roll) {
  const sheet = getOrCreateSheet(ss, 'Leave_Requests', [
    'ReqID', 'Date', 'Roll', 'Name', 'Type', 'TimeWindow', 'Reason',
    'StudentSig', 'SirSig', 'PrincipalSig', 'Status', 'PDF_URL'
  ]);
  const rows = sheet.getDataRange().getValues();
  const out = [];
  for (let i = rows.length - 1; i >= 1; i--) {
    if (String(rows[i][2]).trim() !== String(roll).trim()) continue;
    out.push({
      id: String(rows[i][0] || ''),
      date: String(rows[i][1] || '-'),
      type: String(rows[i][4] || 'Out-Pass'),
      status: String(rows[i][10] || 'PENDING_SIR'),
      pdf: String(rows[i][11] || '')
    });
  }
  return out;
}

function getPendingLeavesForRole(ss, role) {
  const sheet = getOrCreateSheet(ss, 'Leave_Requests', [
    'ReqID', 'Date', 'Roll', 'Name', 'Type', 'TimeWindow', 'Reason',
    'StudentSig', 'SirSig', 'PrincipalSig', 'Status', 'PDF_URL'
  ]);
  const rows = sheet.getDataRange().getValues();
  const targetStatus = (role === 'Principal') ? 'PENDING_PRINCIPAL' : 'PENDING_SIR';
  const out = [];
  for (let i = rows.length - 1; i >= 1; i--) {
    const st = String(rows[i][10] || '').trim().toUpperCase();
    if (st === targetStatus || (role === 'Principal' && st === 'PENDING_SIR')) {
      out.push({
        reqId: String(rows[i][0] || ''),
        date: String(rows[i][1] || '-'),
        roll: String(rows[i][2] || ''),
        name: String(rows[i][3] || 'Trainee'),
        type: String(rows[i][4] || 'Out-Pass'),
        timeWindow: String(rows[i][5] || '-'),
        reason: String(rows[i][6] || '-')
      });
    }
  }
  return out;
}

function handleSubmitLeave(ss, params) {
  const roll = String(params.roll || '').trim();
  const type = String(params.type || 'Out-Pass').trim();
  const timeWindow = String(params.timeWindow || '').trim();
  const reason = String(params.reason || '').trim();
  const studentSig = String(params.studentSig || '').trim();

  const allAttendance = buildAllAttendanceList(ss, todayDateIST());
  const trainee = findTraineeByRoll(allAttendance, roll);
  const name = trainee ? trainee.name : ('Trainee #' + roll);

  const sheet = getOrCreateSheet(ss, 'Leave_Requests', [
    'ReqID', 'Date', 'Roll', 'Name', 'Type', 'TimeWindow', 'Reason',
    'StudentSig', 'SirSig', 'PrincipalSig', 'Status', 'PDF_URL'
  ]);
  const reqId = 'REQ-' + Date.now().toString().slice(-6);
  sheet.appendRow([reqId, todayDateIST(), roll, name, type, timeWindow, reason, studentSig, '', '', 'PENDING_SIR', '']);

  return jsonOut({
    ok: true,
    reqId: reqId,
    msg: 'Request ' + reqId + ' submitted to Trade Instructor.'
  });
}

function handleSignLeave(ss, params, stage) {
  const reqId = String(params.reqId || '').trim();
  const sig = String(stage === 'SIR' ? params.sirSig : params.principalSig || '').trim();
  const sheet = getOrCreateSheet(ss, 'Leave_Requests', [
    'ReqID', 'Date', 'Roll', 'Name', 'Type', 'TimeWindow', 'Reason',
    'StudentSig', 'SirSig', 'PrincipalSig', 'Status', 'PDF_URL'
  ]);
  const rows = sheet.getDataRange().getValues();
  for (let i = 1; i < rows.length; i++) {
    if (String(rows[i][0]).trim() === reqId) {
      if (stage === 'SIR') {
        sheet.getRange(i + 1, 9).setValue(sig);
        sheet.getRange(i + 1, 11).setValue('PENDING_PRINCIPAL');
        return jsonOut({ ok: true, msg: 'Signed by Trade Instructor. Forwarded to Principal.' });
      } else {
        sheet.getRange(i + 1, 10).setValue(sig);
        sheet.getRange(i + 1, 11).setValue('APPROVED');
        const pdfUrl = ss.getUrl();
        sheet.getRange(i + 1, 12).setValue(pdfUrl);
        return jsonOut({ ok: true, pdfUrl: pdfUrl, msg: 'Approved & digitally signed by Principal.' });
      }
    }
  }
  return jsonOut({ ok: false, msg: 'Leave request ' + reqId + ' not found.' });
}

function handleRejectLeave(ss, params, role) {
  const reqId = String(params.reqId || '').trim();
  const reason = String(params.reason || 'Rejected').trim();
  const sheet = getOrCreateSheet(ss, 'Leave_Requests', [
    'ReqID', 'Date', 'Roll', 'Name', 'Type', 'TimeWindow', 'Reason',
    'StudentSig', 'SirSig', 'PrincipalSig', 'Status', 'PDF_URL'
  ]);
  const rows = sheet.getDataRange().getValues();
  for (let i = 1; i < rows.length; i++) {
    if (String(rows[i][0]).trim() === reqId) {
      sheet.getRange(i + 1, 11).setValue('REJECTED (' + role + ': ' + reason + ')');
      return jsonOut({ ok: true, msg: 'Request ' + reqId + ' rejected by ' + role + '.' });
    }
  }
  return jsonOut({ ok: false, msg: 'Request ' + reqId + ' not found.' });
}

function isProjectGroupsEnabled() {
  return PropertiesService.getScriptProperties().getProperty('PROJECT_GROUPS_ENABLED') === 'true';
}

function handleSetProjectGroupsAccess(params) {
  const enabled = String(params.enabled || 'false').toLowerCase() === 'true';
  PropertiesService.getScriptProperties().setProperty('PROJECT_GROUPS_ENABLED', enabled ? 'true' : 'false');
  return jsonOut({
    ok: true,
    projectGroupsEnabled: enabled,
    msg: enabled ? 'Project Groups enabled for trainees.' : 'Project Groups locked by Instructor.'
  });
}

function getProjectGroupsList(ss) {
  const sheet = getOrCreateSheet(ss, 'ProjectGroups', [
    'GroupID', 'ProjectTitle', 'LeaderRoll', 'LeaderName', 'MembersCSV', 'SubmittedByRoll', 'CreatedAt'
  ]);
  const rows = sheet.getDataRange().getValues();
  const out = [];
  for (let i = rows.length - 1; i >= 1; i--) {
    if (!rows[i][1]) continue;
    const members = String(rows[i][4] || '')
      .split(',')
      .map(function (s) { return s.trim(); })
      .filter(Boolean);
    out.push({
      groupId: String(rows[i][0] || ('PG-' + i)),
      projectTitle: String(rows[i][1] || '').trim(),
      leaderRoll: String(rows[i][2] || '').trim(),
      leaderName: String(rows[i][3] || '').trim(),
      members: members,
      submittedByRoll: String(rows[i][5] || '').trim(),
      createdAt: String(rows[i][6] || '').trim()
    });
  }
  return out;
}

function handleProjectGroupsData(ss, params) {
  return jsonOut({
    ok: true,
    projectGroupsEnabled: isProjectGroupsEnabled(),
    projectGroups: getProjectGroupsList(ss)
  });
}

function handleSaveProjectGroup(ss, params) {
  if (!isProjectGroupsEnabled()) {
    return jsonOut({ ok: false, msg: 'Project Groups feature is currently locked by Trade Instructor.' });
  }
  const projectTitle = String(params.projectTitle || '').trim();
  const membersCsv = String(params.members || '').trim();
  const leaderRoll = String(params.leaderRoll || '').trim();
  const leaderName = String(params.leaderName || '').trim();
  const submittedByRoll = String(params.roll || leaderRoll).trim();

  if (!projectTitle) return jsonOut({ ok: false, msg: 'Project title is required.' });
  const members = membersCsv.split(',').map(function (s) { return s.trim(); }).filter(Boolean);
  if (members.length < 1 || members.length > 6) {
    return jsonOut({ ok: false, msg: 'Select between 1 and 6 group members.' });
  }

  const sheet = getOrCreateSheet(ss, 'ProjectGroups', [
    'GroupID', 'ProjectTitle', 'LeaderRoll', 'LeaderName', 'MembersCSV', 'SubmittedByRoll', 'CreatedAt'
  ]);
  const groupId = 'PG-' + Date.now();
  const createdAt = todayDateIST();
  sheet.appendRow([groupId, projectTitle, leaderRoll, leaderName, members.join(','), submittedByRoll, createdAt]);

  return jsonOut({
    ok: true,
    group: {
      groupId: groupId,
      projectTitle: projectTitle,
      leaderRoll: leaderRoll,
      leaderName: leaderName,
      members: members,
      submittedByRoll: submittedByRoll,
      createdAt: createdAt
    },
    msg: 'Project group "' + projectTitle + '" saved successfully.'
  });
}

function handlePrintAttendancePdf(ss) {
  const sheet = ss.getSheetByName('Trainees') || ss.getSheets()[0];
  const ssId = ss.getId();
  const gid = sheet.getSheetId();
  const pdfUrl = 'https://docs.google.com/spreadsheets/d/' + ssId +
    '/export?format=pdf&gid=' + gid + '&portrait=true&fitw=true&gridlines=true';

  return jsonOut({
    ok: true,
    pdfUrl: pdfUrl,
    msg: 'Official Govt. ITI Ranni Attendance Register PDF generated.'
  });
}

// ============================================================================
// 9. INSTRUCTOR MANUAL PUNCH (DUPLICATE GUARD + OVERRIDE) & UNDO ATTENDANCE
// ============================================================================

function statusBucket(st) {
  const s = String(st || '').trim().toUpperCase();
  if (s.indexOf('HALF') !== -1) return 'HALF';
  if (s === 'PRESENT' || s === 'LATE' || s === 'ON-DUTY' || s === 'OUT-PASS') return 'PRESENT';
  return 'ABSENT';
}

function reconcileTraineeTotalsDelta(ss, trainee, oldStatus, newStatus, hadExistingRow) {
  if (!trainee || !trainee.rowIndex) return;
  const sheet = ss.getSheetByName('Trainees');
  if (!sheet) return;
  let p = Math.max(0, Number(sheet.getRange(trainee.rowIndex, 5).getValue()) || 0);
  let h = Math.max(0, Number(sheet.getRange(trainee.rowIndex, 6).getValue()) || 0);
  let t = Math.max(0, Number(sheet.getRange(trainee.rowIndex, 7).getValue()) || 0);

  const oldB = hadExistingRow ? statusBucket(oldStatus) : 'ABSENT';
  const newB = statusBucket(newStatus);

  if (oldB === newB) return;

  if (oldB === 'PRESENT') p = Math.max(0, p - 1);
  if (oldB === 'HALF') h = Math.max(0, h - 1);

  if (newB === 'PRESENT') p += 1;
  if (newB === 'HALF') h += 1;

  sheet.getRange(trainee.rowIndex, 5).setValue(p);
  sheet.getRange(trainee.rowIndex, 6).setValue(h);
  sheet.getRange(trainee.rowIndex, 7).setValue(Math.max(t, p + h));
}

function handleManualPunch(ss, params, role) {
  const dateStr = String(params.date || todayDateIST()).trim();
  const roll = String(params.roll || '').trim();
  const status = String(params.status || 'PRESENT').trim().toUpperCase();
  const allowDuplicate = String(params.allowDuplicate || 'false').trim().toLowerCase() === 'true';
  const note = String(params.note || '').trim();
  const timeStr = nowTimeIST();

  if (!roll) return jsonOut({ ok: false, msg: 'Roll number is required.' });

  const allAttendance = buildAllAttendanceList(ss, dateStr);
  const trainee = findTraineeByRoll(allAttendance, roll);
  const name = trainee ? trainee.name : ('Trainee #' + roll);

  const logSheet = getOrCreateSheet(ss, 'Attendance_Log', [
    'Date', 'Roll', 'Name', 'Status', 'In_Time', 'Out_Time', 'Notes'
  ]);
  const rows = logSheet.getDataRange().getValues();
  let existingRow = -1;
  let prevStatus = 'ABSENT';
  let prevInT = '-';
  let prevOutT = '-';
  for (let i = 1; i < rows.length; i++) {
    if (String(rows[i][0]).trim() === dateStr && String(rows[i][1]).trim() === roll) {
      existingRow = i + 1;
      prevStatus = String(rows[i][3] || 'ABSENT').trim().toUpperCase();
      prevInT = String(rows[i][4] || '-').trim() || '-';
      prevOutT = String(rows[i][5] || '-').trim() || '-';
      break;
    }
  }

  const alreadyPunchedToday = existingRow !== -1 && (
    statusBucket(prevStatus) !== 'ABSENT' || (prevInT !== '-' && prevInT !== '')
  );

  if (alreadyPunchedToday && !allowDuplicate) {
    return jsonOut({
      ok: false,
      duplicateBlocked: true,
      existingStatus: prevStatus,
      existingInTime: prevInT,
      msg: 'Roll #' + roll + ' (' + name + ') already punched today as ' + prevStatus +
        (prevInT !== '-' ? ' at ' + prevInT : '') + '. Confirm "Mark again (by mistake)" to override.'
    });
  }

  const tag = note
    ? ('Manual Punch (' + role + '): ' + note)
    : (allowDuplicate ? ('Manual Override (' + role + ')') : ('Manual Punch (' + role + ')'));

  if (existingRow !== -1) {
    const newInT = (status === 'ABSENT') ? '-' : (prevInT !== '-' ? prevInT : timeStr);
    logSheet.getRange(existingRow, 4).setValue(status);
    logSheet.getRange(existingRow, 5).setValue(newInT);
    logSheet.getRange(existingRow, 7).setValue(tag);
    reconcileTraineeTotalsDelta(ss, trainee, prevStatus, status, true);
  } else {
    const newInT = (status === 'ABSENT') ? '-' : timeStr;
    logSheet.appendRow([dateStr, roll, name, status, newInT, '-', tag]);
    reconcileTraineeTotalsDelta(ss, trainee, 'ABSENT', status, false);
  }

  return jsonOut({
    ok: true,
    roll: roll,
    status: status,
    previousStatus: prevStatus,
    previousInT: prevInT,
    previousOutT: prevOutT,
    time: timeStr,
    msg: (allowDuplicate ? 'Override saved: ' : 'Manual punch saved: ') +
      'Roll #' + roll + ' (' + name + ') marked ' + status + ' at ' + timeStr + '.'
  });
}

function handleUndoAttendance(ss, params, role) {
  const dateStr = formatCellDateIST(params.date || todayDateIST(), todayDateIST());
  const roll = String(params.rollNo || params.roll || '').trim();
  const restoreStatus = String(params.restoreStatus || 'ABSENT').trim().toUpperCase();
  const restoreInT = String(params.restoreInT || '-').trim() || '-';
  const restoreOutT = String(params.restoreOutT || '-').trim() || '-';

  if (!roll) return jsonOut({ ok: false, msg: 'Roll number is required for Undo.' });

  const allAttendance = buildAllAttendanceList(ss, dateStr);
  const trainee = findTraineeByRoll(allAttendance, roll);
  const name = trainee ? trainee.name : ('Trainee #' + roll);

  const logSheet = getOrCreateSheet(ss, 'Attendance_Log', [
    'Date', 'Roll', 'Name', 'Status', 'In_Time', 'Out_Time', 'Notes'
  ]);
  const rows = logSheet.getDataRange().getValues();
  let existingRow = -1;
  let curStatus = 'ABSENT';
  for (let i = 1; i < rows.length; i++) {
    const rDate = formatCellDateIST(rows[i][0], '');
    if (rDate === dateStr && String(rows[i][1]).trim() === roll) {
      existingRow = i + 1;
      curStatus = String(rows[i][3] || 'ABSENT').trim().toUpperCase();
      break;
    }
  }

  if (existingRow !== -1) {
    reconcileTraineeTotalsDelta(ss, trainee, curStatus, restoreStatus, true);
    if (restoreStatus === 'ABSENT' && (restoreInT === '-' || restoreInT === '')) {
      logSheet.deleteRow(existingRow);
    } else {
      logSheet.getRange(existingRow, 4).setValue(restoreStatus);
      logSheet.getRange(existingRow, 5).setValue(restoreInT);
      logSheet.getRange(existingRow, 6).setValue(restoreOutT);
      logSheet.getRange(existingRow, 7).setValue('Undo by ' + role + ' at ' + nowTimeIST());
    }
  }

  return jsonOut({
    ok: true,
    roll: roll,
    date: dateStr,
    restoredStatus: restoreStatus,
    msg: 'Reset ' + name + ' (Roll #' + roll + ') to ' + restoreStatus + ' for ' + dateStr + '.'
  });
}

// ============================================================================
// 10. MONTHLY THEORY & LAB MARKS (EDITABLE + PARENT NOTIFICATION)
// ============================================================================

function getMonthlyMarksList(ss, rollFilter, publishedOnly) {
  const sheet = getOrCreateSheet(ss, 'Monthly_Marks', [
    'ID', 'MonthLabel', 'ExamDate', 'Roll', 'StudentName', 'TheoryMarks', 'TheoryMax',
    'LabMarks', 'LabMax', 'Remarks', 'Published', 'Revised', 'UpdatedAt', 'UpdatedBy'
  ]);
  const rows = sheet.getDataRange().getValues();
  const out = [];
  for (let i = rows.length - 1; i >= 1; i--) {
    const rRoll = String(rows[i][3] || '').trim();
    if (!rRoll) continue;
    if (rollFilter && rRoll !== String(rollFilter).trim()) continue;
    const isPub = String(rows[i][10]).toLowerCase() !== 'false';
    if (publishedOnly && !isPub) continue;
    out.push({
      id: String(rows[i][0] || ('MK-' + i)),
      monthLabel: String(rows[i][1] || 'Monthly Exam').trim(),
      examDate: String(rows[i][2] || '').trim(),
      roll: rRoll,
      studentName: String(rows[i][4] || ('Trainee #' + rRoll)).trim(),
      theoryMarks: Math.max(0, Number(rows[i][5]) || 0),
      theoryMax: Math.max(1, Number(rows[i][6]) || 50),
      labMarks: Math.max(0, Number(rows[i][7]) || 0),
      labMax: Math.max(1, Number(rows[i][8]) || 50),
      remarks: String(rows[i][9] || '').trim(),
      published: isPub,
      isRevised: String(rows[i][11]).toLowerCase() === 'true',
      updatedAt: String(rows[i][12] || '').trim()
    });
  }
  return out;
}

function handleSaveMonthlyMark(ss, params, role) {
  const markId = String(params.markId || '').trim();
  const roll = String(params.roll || '').trim();
  const monthLabel = String(params.monthLabel || '').trim();
  const examDate = String(params.examDate || todayDateIST()).trim();
  const theoryMarks = Math.max(0, Number(params.theoryMarks) || 0);
  const theoryMax = Math.max(1, Number(params.theoryMax) || 50);
  const labMarks = Math.max(0, Number(params.labMarks) || 0);
  const labMax = Math.max(1, Number(params.labMax) || 50);
  const remarks = String(params.remarks || '').trim();
  const publish = String(params.publish || 'true').toLowerCase() !== 'false';
  const notifyParent = String(params.notifyParent || 'true').toLowerCase() !== 'false';

  if (!roll || !monthLabel) {
    return jsonOut({ ok: false, msg: 'Roll number and month label are required.' });
  }

  const allAttendance = buildAllAttendanceList(ss, todayDateIST());
  const trainee = findTraineeByRoll(allAttendance, roll);
  const studentName = trainee ? trainee.name : ('Trainee #' + roll);
  const updatedAt = todayDateIST() + ' ' + nowTimeIST();

  const sheet = getOrCreateSheet(ss, 'Monthly_Marks', [
    'ID', 'MonthLabel', 'ExamDate', 'Roll', 'StudentName', 'TheoryMarks', 'TheoryMax',
    'LabMarks', 'LabMax', 'Remarks', 'Published', 'Revised', 'UpdatedAt', 'UpdatedBy'
  ]);
  const rows = sheet.getDataRange().getValues();
  let existingRow = -1;
  let resolvedId = markId;

  for (let i = 1; i < rows.length; i++) {
    const rowId = String(rows[i][0] || '').trim();
    const rowMonth = String(rows[i][1] || '').trim().toLowerCase();
    const rowRoll = String(rows[i][3] || '').trim();
    if ((markId && rowId === markId) || (rowRoll === roll && rowMonth === monthLabel.toLowerCase())) {
      existingRow = i + 1;
      resolvedId = rowId || ('MK-' + Date.now());
      break;
    }
  }

  const isEdit = existingRow !== -1;
  if (isEdit) {
    sheet.getRange(existingRow, 2, 1, 13).setValues([[
      monthLabel, examDate, roll, studentName, theoryMarks, theoryMax,
      labMarks, labMax, remarks, publish, true, updatedAt, role
    ]]);
  } else {
    resolvedId = 'MK-' + Date.now();
    sheet.appendRow([
      resolvedId, monthLabel, examDate, roll, studentName, theoryMarks, theoryMax,
      labMarks, labMax, remarks, publish, false, updatedAt, role
    ]);
  }

  // Trigger Parent notification in Parent_Alerts when published & notifyParent is enabled
  if (publish && notifyParent) {
    const totalGot = theoryMarks + labMarks;
    const totalMax = theoryMax + labMax;
    const pct = totalMax > 0 ? Math.round((totalGot / totalMax) * 100) : 0;
    const alertType = isEdit ? 'MARKS_UPDATED' : 'MARKS_PUBLISHED';
    const alertMsg = (isEdit ? 'Updated ' : 'Published ') + monthLabel + ' marks for ' + studentName +
      ': Theory ' + theoryMarks + '/' + theoryMax + ', Lab ' + labMarks + '/' + labMax +
      ' (Total ' + totalGot + '/' + totalMax + ' — ' + pct + '%).' +
      (remarks ? ' Remarks: ' + remarks : '');
    const paSheet = getOrCreateSheet(ss, 'Parent_Alerts', ['ID', 'Date', 'Time', 'Roll', 'StudentName', 'AlertType', 'Message']);
    paSheet.appendRow(['PA-MK-' + Date.now(), todayDateIST(), nowTimeIST(), roll, studentName, alertType, alertMsg]);
  }

  return jsonOut({
    ok: true,
    markId: resolvedId,
    isRevised: isEdit,
    msg: (isEdit ? 'Updated ' : 'Published ') + monthLabel + ' Theory & Lab marks for Roll #' + roll + ' (' + studentName + ')' +
      (publish && notifyParent ? ' — Parent notified.' : '.')
  });
}

// ============================================================================
// 11. TRAINEE-ONLY WORK / LAB MARK QUERIES (PRIVATE TO TRAINEE & INSTRUCTOR)
// ============================================================================

function getWorkMarkQueriesList(ss, rollFilter) {
  const sheet = getOrCreateSheet(ss, 'Work_Mark_Queries', [
    'ID', 'Date', 'Time', 'Roll', 'StudentName', 'MonthLabel', 'WorkReference',
    'CurrentMarkNote', 'TraineeNote', 'Status', 'InstructorReply', 'UpdatedAt'
  ]);
  const rows = sheet.getDataRange().getValues();
  const out = [];
  for (let i = rows.length - 1; i >= 1 && out.length < 100; i--) {
    const rRoll = String(rows[i][3] || '').trim();
    if (!rRoll) continue;
    if (rollFilter && rRoll !== String(rollFilter).trim()) continue;
    out.push({
      id: String(rows[i][0] || ('WMQ-' + i)),
      date: String(rows[i][1] || '').trim(),
      time: String(rows[i][2] || '').trim(),
      roll: rRoll,
      studentName: String(rows[i][4] || ('Trainee #' + rRoll)).trim(),
      monthLabel: String(rows[i][5] || '').trim(),
      workReference: String(rows[i][6] || 'Workshop / Lab Practical').trim(),
      currentMarkNote: String(rows[i][7] || '').trim(),
      traineeNote: String(rows[i][8] || '').trim(),
      status: String(rows[i][9] || 'OPEN').trim().toUpperCase(),
      instructorReply: String(rows[i][10] || '').trim(),
      updatedAt: String(rows[i][11] || '').trim()
    });
  }
  return out;
}

function handleSubmitWorkMarkQuery(ss, params) {
  const roll = String(params.roll || '').trim();
  const pin = String(params.pin || '').trim();
  const monthLabel = String(params.monthLabel || '').trim();
  const workReference = String(params.workReference || '').trim();
  const currentMarkNote = String(params.currentMarkNote || '').trim();
  const traineeNote = String(params.traineeNote || '').trim();

  if (!roll || !traineeNote) {
    return jsonOut({ ok: false, msg: 'Roll number and query message are required.' });
  }

  const allAttendance = buildAllAttendanceList(ss, todayDateIST());
  const trainee = findTraineeByRoll(allAttendance, roll);
  if (!trainee) return jsonOut({ ok: false, msg: 'Trainee Roll #' + roll + ' not found.' });
  if (trainee.traineePin && trainee.traineePin !== pin) {
    return jsonOut({ ok: false, msg: 'Invalid Trainee PIN. Only the trainee can submit Work / Lab Mark queries.' });
  }

  const sheet = getOrCreateSheet(ss, 'Work_Mark_Queries', [
    'ID', 'Date', 'Time', 'Roll', 'StudentName', 'MonthLabel', 'WorkReference',
    'CurrentMarkNote', 'TraineeNote', 'Status', 'InstructorReply', 'UpdatedAt'
  ]);
  const id = 'WMQ-' + Date.now();
  const dateStr = todayDateIST();
  const timeStr = nowTimeIST();
  sheet.appendRow([
    id, dateStr, timeStr, roll, trainee.name, monthLabel,
    workReference || 'Workshop / Lab Practical', currentMarkNote, traineeNote,
    'OPEN', '', dateStr + ' ' + timeStr
  ]);

  return jsonOut({
    ok: true,
    queryId: id,
    msg: 'Private Work / Lab mark query sent to your Trade Instructor.'
  });
}

function handleFetchWorkMarkQueries(ss, params) {
  const pin = String(params.pin || '').trim();
  const roll = String(params.roll || '').trim();
  const staffRole = getStaffRoleForPin(pin);
  if (staffRole) {
    return jsonOut({
      ok: true,
      workMarkQueries: getWorkMarkQueriesList(ss, roll)
    });
  }
  if (!roll) {
    return jsonOut({ ok: false, msg: 'Unauthorized query access.' });
  }
  const allAttendance = buildAllAttendanceList(ss, todayDateIST());
  const trainee = findTraineeByRoll(allAttendance, roll);
  if (!trainee || (trainee.traineePin && trainee.traineePin !== pin)) {
    return jsonOut({ ok: false, msg: 'Invalid Trainee PIN.' });
  }
  return jsonOut({
    ok: true,
    myWorkMarkQueries: getWorkMarkQueriesList(ss, roll)
  });
}

function handleReplyWorkMarkQuery(ss, params, role) {
  const queryId = String(params.queryId || '').trim();
  const status = String(params.status || 'REVIEWED').trim().toUpperCase();
  const reply = String(params.reply || '').trim();
  if (!queryId) return jsonOut({ ok: false, msg: 'Query ID is required.' });

  const sheet = getOrCreateSheet(ss, 'Work_Mark_Queries', [
    'ID', 'Date', 'Time', 'Roll', 'StudentName', 'MonthLabel', 'WorkReference',
    'CurrentMarkNote', 'TraineeNote', 'Status', 'InstructorReply', 'UpdatedAt'
  ]);
  const rows = sheet.getDataRange().getValues();
  const updatedAt = todayDateIST() + ' ' + nowTimeIST();
  for (let i = 1; i < rows.length; i++) {
    if (String(rows[i][0] || '').trim() === queryId) {
      sheet.getRange(i + 1, 10).setValue(status);
      sheet.getRange(i + 1, 11).setValue(reply);
      sheet.getRange(i + 1, 12).setValue(updatedAt);
      return jsonOut({
        ok: true,
        msg: 'Reply saved (' + status + ') for Work / Lab mark query ' + queryId + ' by ' + role + '.'
      });
    }
  }
  return jsonOut({ ok: false, msg: 'Work / Lab mark query ' + queryId + ' not found.' });
}

// ============================================================================
// 12. TRAINEE "RUNNING LATE" ALERTS TO TRADE INSTRUCTOR
// ============================================================================

function getRunningLateAlertsList(ss, rollFilter) {
  const sheet = getOrCreateSheet(ss, 'Running_Late_Alerts', [
    'ID', 'Date', 'Time', 'Roll', 'StudentName', 'ExpectedArrival', 'ReasonCategory', 'Details', 'Status'
  ]);
  const rows = sheet.getDataRange().getValues();
  const out = [];
  for (let i = rows.length - 1; i >= 1 && out.length < 60; i--) {
    const rRoll = String(rows[i][3] || '').trim();
    if (!rRoll) continue;
    if (rollFilter && rRoll !== String(rollFilter).trim()) continue;
    out.push({
      id: String(rows[i][0] || ('RL-' + i)),
      date: String(rows[i][1] || '').trim(),
      time: String(rows[i][2] || '').trim(),
      roll: rRoll,
      studentName: String(rows[i][4] || ('Trainee #' + rRoll)).trim(),
      expectedArrival: String(rows[i][5] || '').trim(),
      reasonCategory: String(rows[i][6] || 'Delay').trim(),
      details: String(rows[i][7] || '').trim(),
      status: String(rows[i][8] || 'ACTIVE').trim().toUpperCase()
    });
  }
  return out;
}

function handleSubmitRunningLate(ss, params) {
  const roll = String(params.roll || '').trim();
  const pin = String(params.pin || '').trim();
  const expectedArrival = String(params.expectedArrival || '').trim();
  const reasonCategory = String(params.reasonCategory || 'Bus / Transport Delay').trim();
  const details = String(params.details || '').trim();

  if (!roll || !expectedArrival) {
    return jsonOut({ ok: false, msg: 'Roll number and expected arrival / delay window are required.' });
  }

  const allAttendance = buildAllAttendanceList(ss, todayDateIST());
  const trainee = findTraineeByRoll(allAttendance, roll);
  if (!trainee) return jsonOut({ ok: false, msg: 'Trainee Roll #' + roll + ' not found.' });
  if (trainee.traineePin && trainee.traineePin !== pin) {
    return jsonOut({ ok: false, msg: 'Invalid Trainee PIN.' });
  }

  const sheet = getOrCreateSheet(ss, 'Running_Late_Alerts', [
    'ID', 'Date', 'Time', 'Roll', 'StudentName', 'ExpectedArrival', 'ReasonCategory', 'Details', 'Status'
  ]);
  const id = 'RL-' + Date.now();
  const dateStr = todayDateIST();
  const timeStr = nowTimeIST();
  sheet.appendRow([id, dateStr, timeStr, roll, trainee.name, expectedArrival, reasonCategory, details, 'ACTIVE']);

  return jsonOut({
    ok: true,
    alertId: id,
    msg: 'Running Late alert sent to Trade Instructor (ETA: ' + expectedArrival + ').'
  });
}

// ============================================================================
// 13. THEORY NOTES & LAB LOG BOOK SUBMISSION & VERIFICATION (Academic_Submissions)
// ============================================================================

const ACADEMIC_SUBMISSIONS_HEADERS = [
  'ID', 'Roll', 'StudentName', 'TheoryTopic', 'TheoryStatus', 'TheoryUpdatedDate',
  'LabJobName', 'LabStatus', 'LabSignedDate', 'Remarks', 'SubmittedAt',
  'VerificationStatus', 'VerifiedBy', 'VerifiedAt', 'InstructorRemark', 'SentToPrincipal', 'ForwardedAt'
];

function getAcademicSubmissionsList(ss, rollFilter) {
  const sheet = getOrCreateSheet(ss, 'Academic_Submissions', ACADEMIC_SUBMISSIONS_HEADERS);
  const rows = sheet.getDataRange().getValues();
  const byRoll = {};
  for (let i = 1; i < rows.length; i++) {
    const rRoll = String(rows[i][1] || '').trim();
    if (!rRoll) continue;
    if (rollFilter && rRoll !== String(rollFilter).trim()) continue;
    const sentFlag = String(rows[i][15]).toLowerCase() === 'true';
    let verStatus = String(rows[i][11] || 'PENDING_VERIFICATION').trim().toUpperCase();
    if (sentFlag) verStatus = 'FORWARDED_TO_PRINCIPAL';
    byRoll[rRoll] = {
      id: String(rows[i][0] || ('ACAD-' + rRoll)),
      roll: rRoll,
      studentName: String(rows[i][2] || ('Trainee #' + rRoll)).trim(),
      theoryTopic: String(rows[i][3] || '').trim(),
      theoryStatus: String(rows[i][4] || 'Pending').trim(),
      theoryUpdatedDate: formatCellDateIST(rows[i][5], '-'),
      labJobName: String(rows[i][6] || '').trim(),
      labStatus: String(rows[i][7] || 'Incomplete').trim(),
      labSignedDate: formatCellDateIST(rows[i][8], ''),
      remarks: String(rows[i][9] || '').trim(),
      submittedAt: formatCellDateTimeIST(rows[i][10]),
      verificationStatus: verStatus,
      verifiedBy: String(rows[i][12] || '').trim(),
      verifiedAt: formatCellDateTimeIST(rows[i][13]),
      instructorRemark: String(rows[i][14] || '').trim(),
      sentToPrincipal: sentFlag,
      forwardedAt: formatCellDateTimeIST(rows[i][16])
    };
  }
  return Object.keys(byRoll)
    .map(function (k) { return byRoll[k]; })
    .sort(function (a, b) { return (Number(a.roll) || 999) - (Number(b.roll) || 999); });
}

function handleSubmitAcademicRecord(ss, params) {
  const roll = String(params.rollNo || params.roll || '').trim();
  const pin = String(params.pin || '').trim();
  if (!roll) return jsonOut({ ok: false, msg: 'Roll number is required.' });

  const allAttendance = buildAllAttendanceList(ss, todayDateIST());
  const trainee = findTraineeByRoll(allAttendance, roll);
  if (!trainee) return jsonOut({ ok: false, msg: 'Trainee Roll #' + roll + ' not found.' });
  if (trainee.traineePin && trainee.traineePin !== pin && !getStaffRoleForPin(pin)) {
    return jsonOut({ ok: false, msg: 'Invalid Trainee PIN.' });
  }

  const studentName = String(params.studentName || trainee.name || ('Trainee #' + roll)).trim();
  const theoryTopic = String(params.theoryTopic || '').trim();
  const theoryStatus = String(params.theoryStatus || 'In Progress').trim();
  const theoryUpdatedDate = formatCellDateIST(params.theoryUpdatedDate || todayDateIST(), todayDateIST());
  const labJobName = String(params.labJobName || '').trim();
  const labStatus = String(params.labStatus || 'Submitted for Sign').trim();
  const labSignedDate = params.labSignedDate ? formatCellDateIST(params.labSignedDate, '') : '';
  const remarks = String(params.remarks || '').trim();
  const submittedAt = nowDateTimeIST();

  const sheet = getOrCreateSheet(ss, 'Academic_Submissions', ACADEMIC_SUBMISSIONS_HEADERS);
  const rows = sheet.getDataRange().getValues();
  let existingRow = -1;
  let existingId = 'ACAD-' + roll + '-' + Date.now();
  for (let i = 1; i < rows.length; i++) {
    if (String(rows[i][1] || '').trim() === roll) {
      existingRow = i + 1;
      existingId = String(rows[i][0] || existingId);
      break;
    }
  }

  const rowValues = [
    existingId, roll, studentName, theoryTopic, theoryStatus, theoryUpdatedDate,
    labJobName, labStatus, labSignedDate, remarks, submittedAt,
    'PENDING_VERIFICATION', '', '', '', false, ''
  ];
  if (existingRow !== -1) {
    sheet.getRange(existingRow, 1, 1, rowValues.length).setValues([rowValues]);
  } else {
    sheet.appendRow(rowValues);
  }

  return jsonOut({
    ok: true,
    submissionId: existingId,
    submittedAt: submittedAt,
    msg: 'Theory Notes & Lab Log Book submitted for Instructor verification.'
  });
}

function handleGetAcademicSubmissions(ss, params) {
  const pin = String(params.pin || '').trim();
  const roll = String(params.rollNo || params.roll || '').trim();
  const staffRole = getStaffRoleForPin(pin);
  if (staffRole) {
    return jsonOut({
      ok: true,
      submissions: getAcademicSubmissionsList(ss, roll)
    });
  }
  if (!roll) return jsonOut({ ok: false, msg: 'Unauthorized access to Academic Submissions.' });
  const allAttendance = buildAllAttendanceList(ss, todayDateIST());
  const trainee = findTraineeByRoll(allAttendance, roll);
  if (!trainee || (trainee.traineePin && trainee.traineePin !== pin)) {
    return jsonOut({ ok: false, msg: 'Invalid Trainee PIN.' });
  }
  return jsonOut({
    ok: true,
    submissions: getAcademicSubmissionsList(ss, roll)
  });
}

function handleVerifyAcademicSubmission(ss, params, role) {
  const roll = String(params.rollNo || params.roll || '').trim();
  if (!roll) return jsonOut({ ok: false, msg: 'Roll number is required.' });
  const instructorRemark = String(params.instructorRemark || '').trim();
  const verifiedBy = String(params.verifiedBy || 'Trade Instructor (EM)').trim();
  const verifiedAt = nowDateTimeIST();

  const sheet = getOrCreateSheet(ss, 'Academic_Submissions', ACADEMIC_SUBMISSIONS_HEADERS);
  const rows = sheet.getDataRange().getValues();
  let existingRow = -1;
  for (let i = 1; i < rows.length; i++) {
    if (String(rows[i][1] || '').trim() === roll) {
      existingRow = i + 1;
      break;
    }
  }

  if (existingRow === -1) {
    const allAttendance = buildAllAttendanceList(ss, todayDateIST());
    const trainee = findTraineeByRoll(allAttendance, roll);
    const name = trainee ? trainee.name : ('Trainee #' + roll);
    sheet.appendRow([
      'ACAD-' + roll + '-' + Date.now(), roll, name,
      'Verified in Workshop', 'Completed', todayDateIST(),
      'Lab Log Verified', 'Signed by Instructor', todayDateIST(),
      '', verifiedAt, 'VERIFIED', verifiedBy, verifiedAt, instructorRemark, false, ''
    ]);
  } else {
    sheet.getRange(existingRow, 12).setValue('VERIFIED');
    sheet.getRange(existingRow, 13).setValue(verifiedBy);
    sheet.getRange(existingRow, 14).setValue(verifiedAt);
    sheet.getRange(existingRow, 15).setValue(instructorRemark);
  }

  return jsonOut({
    ok: true,
    verifiedAt: verifiedAt,
    msg: 'Verified & signed Roll #' + roll + ' Theory Notes & Lab Log Book (' + verifiedBy + ').'
  });
}

function handleForwardAcademicToPrincipal(ss, params, role) {
  const roll = String(params.rollNo || params.roll || '').trim();
  const bulk = String(params.bulk || 'false').toLowerCase() === 'true';
  const forwardedAt = nowDateTimeIST();

  const sheet = getOrCreateSheet(ss, 'Academic_Submissions', ACADEMIC_SUBMISSIONS_HEADERS);
  const rows = sheet.getDataRange().getValues();
  let forwardedCount = 0;

  for (let i = 1; i < rows.length; i++) {
    const rRoll = String(rows[i][1] || '').trim();
    const curStatus = String(rows[i][11] || '').trim().toUpperCase();
    const alreadySent = String(rows[i][15]).toLowerCase() === 'true';

    if (bulk) {
      if (curStatus === 'VERIFIED' && !alreadySent) {
        sheet.getRange(i + 1, 12).setValue('FORWARDED_TO_PRINCIPAL');
        sheet.getRange(i + 1, 16).setValue(true);
        sheet.getRange(i + 1, 17).setValue(forwardedAt);
        forwardedCount++;
      }
    } else if (rRoll === roll) {
      sheet.getRange(i + 1, 12).setValue('FORWARDED_TO_PRINCIPAL');
      sheet.getRange(i + 1, 16).setValue(true);
      sheet.getRange(i + 1, 17).setValue(forwardedAt);
      forwardedCount++;
      break;
    }
  }

  if (forwardedCount === 0) {
    return jsonOut({
      ok: false,
      msg: bulk
        ? 'No newly verified records waiting to be forwarded to Principal.'
        : 'Record for Roll #' + roll + ' not found. Please verify it first.'
    });
  }

  return jsonOut({
    ok: true,
    forwardedCount: forwardedCount,
    forwardedAt: forwardedAt,
    msg: bulk
      ? 'Forwarded ' + forwardedCount + ' verified record(s) to Principal.'
      : 'Forwarded Roll #' + roll + ' verified academic record to Principal.'
  });
}
