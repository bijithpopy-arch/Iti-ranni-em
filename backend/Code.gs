/**
 * Govt. ITI Ranni — Electronics Mechanic (2025–2027)
 * Google Apps Script Backend Handlers for Attendance, Confirm & Close,
 * Holiday Announcements, Trade Notices, Parent Portal/Alerts, and Project Groups.
 *
 * Add/merge these sheets and action cases inside your existing `doGet(e)` router:
 *   Sheets auto-created if missing:
 *   - DailyCloseLog  : [Date, Closed, ClosedAt, ClosedBy, AbsentCount, HalfDayCount]
 *   - Holidays       : [ID, Date, Reason, PostedBy, PostedAt]
 *   - Notices        : [ID, Date, Title, Category, Body, PostedBy]
 *   - ParentAlerts   : [ID, Date, Time, Roll, StudentName, AlertType, Message]
 *   - ProjectGroups  : [GroupID, ProjectTitle, LeaderRoll, LeaderName, MembersCSV, SubmittedByRoll, CreatedAt]
 *
 * ScriptProperties key:
 *   - PROJECT_GROUPS_ENABLED = "true" | "false"
 */

const TZ = 'Asia/Kolkata';

function todayDateIST() {
  return Utilities.formatDate(new Date(), TZ, 'dd-MM-yyyy');
}

function nowTimeIST() {
  return Utilities.formatDate(new Date(), TZ, 'hh:mm a');
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

/**
 * Check if today's attendance has been confirmed & closed by the Trade Instructor.
 */
function getDailyCloseState(ss, dateStr) {
  const sheet = getOrCreateSheet(ss, 'DailyCloseLog', ['Date', 'Closed', 'ClosedAt', 'ClosedBy', 'AbsentCount', 'HalfDayCount']);
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

/**
 * Action: confirm_close_attendance&pin=<PIN>&date=<DD-MM-YYYY>
 */
function handleConfirmCloseAttendance(ss, params, allAttendance) {
  const dateStr = (params.date || todayDateIST()).trim();
  const closedAt = nowTimeIST();

  const todayHoliday = getHolidayForDate(ss, dateStr);
  let absentCount = 0;
  let halfDayCount = 0;

  (allAttendance || []).forEach(r => {
    const st = String(r.todayStatus || '').toUpperCase();
    if (st === 'ABSENT' || st === '') absentCount++;
    else if (st.indexOf('HALF') !== -1) halfDayCount++;
  });

  const sheet = getOrCreateSheet(ss, 'DailyCloseLog', ['Date', 'Closed', 'ClosedAt', 'ClosedBy', 'AbsentCount', 'HalfDayCount']);
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
    ? `Attendance closed for ${dateStr} (Declared Holiday — ABSENT alerts skipped).`
    : `Today's attendance confirmed & closed at ${closedAt}. Parent alerts enabled for ${absentCount} Absent & ${halfDayCount} Half-Day trainees.`;

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

/**
 * Holidays helpers & Action: post_holiday&pin=<PIN>&date=<DD-MM-YYYY>&reason=<REASON>
 */
function getHolidaysList(ss) {
  const sheet = getOrCreateSheet(ss, 'Holidays', ['ID', 'Date', 'Reason', 'PostedBy', 'PostedAt']);
  const rows = sheet.getDataRange().getValues();
  const out = [];
  for (let i = rows.length - 1; i >= 1; i--) {
    if (!rows[i][1]) continue;
    out.push({
      id: String(rows[i][0] || `HOL-${i}`),
      date: String(rows[i][1]).trim(),
      reason: String(rows[i][2] || '').trim(),
      postedBy: String(rows[i][3] || 'Trade Instructor'),
      postedAt: String(rows[i][4] || '')
    });
  }
  return out;
}

function getHolidayForDate(ss, dateStr) {
  const list = getHolidaysList(ss);
  return list.find(h => h.date === dateStr) || null;
}

function handlePostHoliday(ss, params) {
  const dateStr = (params.date || todayDateIST()).trim();
  const reason = (params.reason || '').trim();
  if (!reason) return jsonOut({ ok: false, msg: 'Holiday reason is required.' });

  const sheet = getOrCreateSheet(ss, 'Holidays', ['ID', 'Date', 'Reason', 'PostedBy', 'PostedAt']);
  const id = 'HOL-' + Date.now().toString().slice(-6);
  const postedAt = nowTimeIST();
  sheet.appendRow([id, dateStr, reason, 'Trade Instructor', postedAt]);

  return jsonOut({
    ok: true,
    holiday: { id, date: dateStr, reason, postedBy: 'Trade Instructor', postedAt },
    msg: `Holiday announced for ${dateStr}. False ABSENT alerts will be skipped.`
  });
}

/**
 * Notices helpers & Action: post_notice&pin=<PIN>&title=...&category=...&body=...&date=...
 */
function getNoticesList(ss) {
  const sheet = getOrCreateSheet(ss, 'Notices', ['ID', 'Date', 'Title', 'Category', 'Body', 'PostedBy']);
  const rows = sheet.getDataRange().getValues();
  const out = [];
  for (let i = rows.length - 1; i >= 1; i--) {
    if (!rows[i][2]) continue;
    out.push({
      id: String(rows[i][0] || `NTC-${i}`),
      date: String(rows[i][1] || ''),
      title: String(rows[i][2] || ''),
      category: String(rows[i][3] || 'General'),
      body: String(rows[i][4] || ''),
      postedBy: String(rows[i][5] || 'Govt. ITI Ranni')
    });
  }
  return out;
}

function handlePostNotice(ss, params) {
  const title = (params.title || '').trim();
  const body = (params.body || '').trim();
  const category = (params.category || 'General').trim();
  const dateStr = (params.date || todayDateIST()).trim();
  if (!title || !body) return jsonOut({ ok: false, msg: 'Notice title and message body are required.' });

  const sheet = getOrCreateSheet(ss, 'Notices', ['ID', 'Date', 'Title', 'Category', 'Body', 'PostedBy']);
  const id = 'NTC-' + Date.now().toString().slice(-7);
  sheet.appendRow([id, dateStr, title, category, body, 'Govt. ITI Ranni']);

  return jsonOut({
    ok: true,
    notice: { id, date: dateStr, title, category, body, postedBy: 'Govt. ITI Ranni' },
    msg: 'Notice published successfully.'
  });
}

/**
 * Parent Alerts helpers & Actions: parent_data, parent_submit_alert
 */
function getParentAlertsList(ss, filterRoll) {
  const sheet = getOrCreateSheet(ss, 'ParentAlerts', ['ID', 'Date', 'Time', 'Roll', 'StudentName', 'AlertType', 'Message']);
  const rows = sheet.getDataRange().getValues();
  const out = [];
  for (let i = rows.length - 1; i >= 1; i--) {
    const roll = String(rows[i][3] || '').trim();
    if (!roll) continue;
    if (filterRoll && roll !== String(filterRoll).trim()) continue;
    out.push({
      id: String(rows[i][0] || `PA-${i}`),
      date: String(rows[i][1] || ''),
      time: String(rows[i][2] || ''),
      roll: roll,
      studentName: String(rows[i][4] || `Trainee #${roll}`),
      alertType: String(rows[i][5] || 'LEAVE_REASON'),
      message: String(rows[i][6] || '')
    });
  }
  return out;
}

function handleParentSubmitAlert(ss, params, studentName) {
  const roll = String(params.roll || '').trim();
  const alertType = (params.alertType || 'LEAVE_REASON').trim().toUpperCase();
  const message = (params.message || '').trim();
  if (!roll || !message) return jsonOut({ ok: false, msg: 'Roll number and message are required.' });

  const sheet = getOrCreateSheet(ss, 'ParentAlerts', ['ID', 'Date', 'Time', 'Roll', 'StudentName', 'AlertType', 'Message']);
  const id = 'PA-' + Date.now().toString().slice(-6);
  const dateStr = todayDateIST();
  const timeStr = nowTimeIST();
  sheet.appendRow([id, dateStr, timeStr, roll, studentName || `Trainee #${roll}`, alertType, message]);

  return jsonOut({
    ok: true,
    alertId: id,
    msg: alertType === 'BUNK_ALERT'
      ? 'Urgent bunking alert sent to Trade Instructor.'
      : 'Leave reason submitted to Trade Instructor.'
  });
}

/**
 * Project Groups helpers & Actions:
 *   - set_project_groups_access&pin=<PIN>&enabled=true|false
 *   - save_project_group&roll=<ROLL>&pin=<PIN>&projectTitle=...&members=1,2,3,4&leaderRoll=1&leaderName=...
 *   - project_groups_data&pin=<PIN>[&roll=<ROLL>]
 */
function isProjectGroupsEnabled() {
  return PropertiesService.getScriptProperties().getProperty('PROJECT_GROUPS_ENABLED') === 'true';
}

function getProjectGroupsList(ss) {
  const sheet = getOrCreateSheet(ss, 'ProjectGroups', ['GroupID', 'ProjectTitle', 'LeaderRoll', 'LeaderName', 'MembersCSV', 'SubmittedByRoll', 'CreatedAt']);
  const rows = sheet.getDataRange().getValues();
  const out = [];
  for (let i = rows.length - 1; i >= 1; i--) {
    if (!rows[i][1]) continue;
    const members = String(rows[i][4] || '').split(',').map(s => s.trim()).filter(Boolean);
    out.push({
      groupId: String(rows[i][0] || `PG-${i}`),
      projectTitle: String(rows[i][1] || ''),
      leaderRoll: String(rows[i][2] || ''),
      leaderName: String(rows[i][3] || ''),
      members: members,
      submittedByRoll: String(rows[i][5] || ''),
      createdAt: String(rows[i][6] || '')
    });
  }
  return out;
}

function handleSetProjectGroupsAccess(params) {
  const enabled = String(params.enabled).toLowerCase() === 'true';
  PropertiesService.getScriptProperties().setProperty('PROJECT_GROUPS_ENABLED', enabled ? 'true' : 'false');
  return jsonOut({
    ok: true,
    projectGroupsEnabled: enabled,
    msg: enabled
      ? 'Project Groups tab enabled for trainees.'
      : 'Project Groups tab locked for trainees.'
  });
}

function handleSaveProjectGroup(ss, params) {
  if (!isProjectGroupsEnabled()) {
    return jsonOut({ ok: false, msg: 'Project Groups submission is currently locked by Trade Instructor.' });
  }
  const projectTitle = (params.projectTitle || '').trim();
  const leaderRoll = String(params.leaderRoll || '').trim();
  const leaderName = (params.leaderName || '').trim();
  const members = String(params.members || '')
    .split(',')
    .map(s => s.trim())
    .filter(Boolean);

  if (!projectTitle) return jsonOut({ ok: false, msg: 'Project Title is required.' });
  if (members.length < 1 || members.length > 4) {
    return jsonOut({ ok: false, msg: 'A project group must have between 1 and 4 members maximum.' });
  }
  if (!leaderRoll || members.indexOf(leaderRoll) === -1) {
    return jsonOut({ ok: false, msg: 'Exactly 1 Leader must be selected from the group members.' });
  }

  const sheet = getOrCreateSheet(ss, 'ProjectGroups', ['GroupID', 'ProjectTitle', 'LeaderRoll', 'LeaderName', 'MembersCSV', 'SubmittedByRoll', 'CreatedAt']);
  const groupId = 'PG-' + Date.now().toString().slice(-6);
  const createdAt = todayDateIST();
  sheet.appendRow([groupId, projectTitle, leaderRoll, leaderName, members.join(','), String(params.roll || ''), createdAt]);

  return jsonOut({
    ok: true,
    group: {
      groupId,
      projectTitle,
      leaderRoll,
      leaderName,
      members,
      submittedByRoll: String(params.roll || ''),
      createdAt
    },
    msg: `Project group "${projectTitle}" saved (Leader: Roll #${leaderRoll}).`
  });
}
