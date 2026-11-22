                signedAt = json.optString("signedAt", "").trim(),
                note = note.trim()
            )
            DutySignOffResult(
                record = rec,
                message = json.optString("msg", "Duty verified & signed off for $pairLabel.")
                    .ifBlank { "Duty verified & signed off for $pairLabel." }
            )
        }
    }

    suspend fun logToolCribIssue(
        pin: String,
        itemName: String,
        conditionStatus: String,
        notes: String
    ): ApiResult<ToolCribLogResult> {
        return executeGet(
            mapOf(
                "action" to "log_tool_crib",
                "pin" to pin.trim(),
                "item" to itemName.trim(),
                "status" to conditionStatus.trim(),
                "notes" to notes.trim()
            )
        ) { json ->
            val entry = ToolCribLogItem(
                id = json.optString("id", System.currentTimeMillis().toString()).trim(),
                roll = json.optSafeString("roll"),
                name = json.optString("name", "").trim(),
                tool = itemName.trim(),
                status = conditionStatus.trim(),
                time = json.optString("time", todayIstDateString()).trim()
            )
            ToolCribLogResult(
                entry = entry,
                message = json.optString("msg", "Tool crib log recorded.").ifBlank { "Tool crib log recorded." }
            )
        }
    }

    suspend fun sendInstructorParentAlert(
        pin: String,
        roll: String,
        alertType: String,
        message: String
    ): ApiResult<SimpleActionResult> {
        return executeGet(
            mapOf(
                "action" to "instructor_parent_alert",
                "pin" to pin.trim(),
                "roll" to roll.trim(),
                "alertType" to alertType.trim(),
                "message" to message.trim()
            )
        ) { json ->
            SimpleActionResult(
                message = json.optString("msg", "Parent alert dispatched for Roll #$roll.")
                    .ifBlank { "Parent alert dispatched for Roll #$roll." }
            )
        }
    }

    suspend fun confirmCloseAttendance(
        pin: String,
        date: String = todayIstDateString()
    ): ApiResult<CloseAttendanceResult> {
        return executeGet(
            mapOf(
                "action" to "confirm_close_attendance",
                "pin" to pin.trim(),
                "date" to date.trim()
            )
        ) { json ->
            CloseAttendanceResult(
                attendanceClosed = json.optBoolean("attendanceClosed", true),
                date = json.optString("date", date).ifBlank { date },
                closedAt = json.optString("closedAt", "").trim(),
                message = json.optString("msg", "Today's attendance confirmed and locked.")
                    .ifBlank { "Today's attendance confirmed and locked." }
            )
        }
    }

    suspend fun postHoliday(
        pin: String,
        date: String,
        reason: String
    ): ApiResult<HolidayPostResult> {
        return executeGet(
            mapOf(
                "action" to "post_holiday",
                "pin" to pin.trim(),
                "date" to date.trim(),
                "reason" to reason.trim()
            )
        ) { json ->
            val holiday = HolidayItem(
                date = date.trim(),
                title = reason.trim()
            )
            HolidayPostResult(
                holiday = holiday,
                message = json.optString("msg", "Holiday announced for $date.")
                    .ifBlank { "Holiday announced for $date." }
            )
        }
    }

    suspend fun postNotice(
        pin: String,
        title: String,
        category: String,
        body: String,
        pinned: Boolean = false,
        postedBy: String = "Trade Instructor (EM)"
    ): ApiResult<NoticePostResult> {
        return executeGet(
            mapOf(
                "action" to "post_notice",
                "pin" to pin.trim(),
                "title" to title.trim(),
                "category" to category.trim(),
                "body" to body.trim(),
                "pinned" to pinned.toString(),
                "postedBy" to postedBy.trim()
            )
        ) { json ->
            val notice = NoticeItem(
                id = json.optString("id", System.currentTimeMillis().toString()).trim(),
                title = title.trim(),
                body = body.trim(),
                date = json.optString("date", todayIstDateString()).trim(),
                isPinned = pinned
            )
            NoticePostResult(
                notice = notice,
                message = json.optString("msg", "Notice published.").ifBlank { "Notice published." }
            )
        }
    }

    suspend fun setProjectGroupsAccess(
        pin: String,
        enabled: Boolean
    ): ApiResult<ProjectGroupsAccessResult> {
        return executeGet(
            mapOf(
                "action" to "set_project_groups_access",
                "pin" to pin.trim(),
                "enabled" to enabled.toString()
            )
        ) { json ->
            ProjectGroupsAccessResult(
                enabled = json.optBoolean("enabled", enabled),
                message = json.optString("msg", "Project groups access updated.")
                    .ifBlank { "Project groups access updated." }
            )
        }
    }

    suspend fun fetchProjectGroups(
        pin: String,
        roll: String = ""
    ): ApiResult<ProjectGroupsFetchResult> {
        return executeGet(
            mapOf(
                "action" to "fetch_project_groups",
                "pin" to pin.trim(),
                "roll" to roll.trim()
            )
        ) { json ->
            ProjectGroupsFetchResult(
                enabled = json.optBoolean("projectGroupsEnabled", false),
                groups = parseProjectGroups(json.optJSONArray("projectGroups"))
            )
        }
    }

    suspend fun saveProjectGroup(
        roll: String,
        pin: String,
        projectTitle: String,
        memberRolls: List<Int>,
        leaderRoll: Int,
        leaderName: String
    ): ApiResult<ProjectGroupSaveResult> {
        return executeGet(
            mapOf(
                "action" to "save_project_group",
                "roll" to roll.trim(),
                "pin" to pin.trim(),
                "projectTitle" to projectTitle.trim(),
                "memberRolls" to memberRolls.joinToString(","),
                "leaderRoll" to leaderRoll.toString(),
                "leaderName" to leaderName.trim()
            )
        ) { json ->
            val groupObj = json.optJSONObject("group")
            val group = ProjectGroupItem(
                id = groupObj?.optString("id", "")?.trim().orEmpty(),
                title = groupObj?.optString("title", projectTitle)?.trim().orEmpty(),
                members = groupObj?.optString("members", memberRolls.joinToString(", "))?.trim().orEmpty(),
                status = groupObj?.optString("status", "SUBMITTED")?.trim().orEmpty()
            )
            ProjectGroupSaveResult(
                group = group,
                message = json.optString("msg", "Project group saved.").ifBlank { "Project group saved." }
            )
        }
    }

    suspend fun signByInstructor(
        pin: String,
        reqId: String,
        sirSig: String
    ): ApiResult<LeaveSignResult> {
        return executeGet(
            mapOf(
                "action" to "sign_by_instructor",
                "pin" to pin.trim(),
                "reqId" to reqId.trim(),
                "sirSig" to sirSig.trim()
            )
        ) { json ->
            LeaveSignResult(
                pdfUrl = json.optString("pdfUrl", json.optString("pdf", "")).trim(),
                message = json.optString("msg", "Approved and signed by Trade Instructor.")
                    .ifBlank { "Approved and signed by Trade Instructor." }
            )
        }
    }

    suspend fun signByPrincipal(
        pin: String,
        reqId: String,
        principalSig: String
    ): ApiResult<LeaveSignResult> {
        return executeGet(
            mapOf(
                "action" to "sign_by_principal",
                "pin" to pin.trim(),
                "reqId" to reqId.trim(),
                "principalSig" to principalSig.trim()
            )
        ) { json ->
            LeaveSignResult(
                pdfUrl = json.optString("pdfUrl", json.optString("pdf", "")).trim(),
                message = json.optString("msg", "Approved and signed by Principal.")
                    .ifBlank { "Approved and signed by Principal." }
            )
        }
    }

    suspend fun rejectLeaveRequest(
        pin: String,
        reqId: String,
        reason: String
    ): ApiResult<SimpleActionResult> {
        return executeGet(
            mapOf(
                "action" to "reject_leave",
                "pin" to pin.trim(),
                "reqId" to reqId.trim(),
                "reason" to reason.trim()
            )
        ) { json ->
            SimpleActionResult(
                message = json.optString("msg", "Leave request rejected.")
                    .ifBlank { "Leave request rejected." }
            )
        }
    }

    suspend fun exportDailyAttendancePdf(pin: String): ApiResult<PdfExportResult> {
        return executeGet(
            mapOf(
                "action" to "export_daily_pdf",
                "pin" to pin.trim(),
                "date" to todayIstDateString()
            )
        ) { json ->
            PdfExportResult(
                pdfUrl = json.optString("pdfUrl", json.optString("url", "")).trim()
            )
        }
    }

    suspend fun triggerEspTap(roll: String): ApiResult<EspTapResult> {
        return executeGet(
            mapOf(
                "action" to "esp_tap",
                "key" to ESP_KEY,
                "roll" to roll.trim()
            )
        ) { json ->
            EspTapResult(
                roll = roll.trim(),
                mode = json.optString("mode", "IN").ifBlank { "IN" },
                name = json.optString("name", "Roll #$roll").ifBlank { "Roll #$roll" },
                time = json.optString("time", "-").ifBlank { "-" },
                message = json.optString("msg", "ID-card tap recorded.").trim()
            )
        }
    }
    private suspend fun <T> executeGet(
        params: Map<String, String>,
        parser: (JSONObject) -> T
    ): ApiResult<T> = withContext(Dispatchers.IO) {
        try {
            val urlBuilder = BASE_URL.toHttpUrl().newBuilder()
            params.forEach { (k, v) -> urlBuilder.addQueryParameter(k, v) }
            val request = Request.Builder().url(urlBuilder.build()).get().build()
            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext ApiResult.Error("Server error (${response.code}).")
                }
                val json = JSONObject(raw)
                if (!json.optBoolean("ok", false)) {
                    val errMsg = json.optString("msg", json.optString("error", "Request not found"))
                        .ifBlank { "Request not found" }
                    return@withContext ApiResult.Error(errMsg)
                }
                ApiResult.Success(parser(json))
            }
        } catch (e: IOException) {
            ApiResult.Error("Network error: ${e.localizedMessage ?: "check connection"}")
        } catch (e: Exception) {
            ApiResult.Error("Response error: ${e.localizedMessage ?: "invalid data"}")
        }
    }

    private fun parseAttendanceSummary(obj: JSONObject?): AttendanceSummary {
        if (obj == null) return AttendanceSummary()
        val present = obj.optSafeInt("present").coerceAtLeast(0)
        val half = obj.optSafeInt("half").coerceAtLeast(0)
        val total = AttendanceMath.safeTotalDays(present, half, obj.optSafeInt("total"))
        val pct = AttendanceMath.computePercentage(present, half, total, obj.optSafeInt("pct"))
        return AttendanceSummary(
            todayStatus = obj.optString("todayStatus", "ABSENT").ifBlank { "ABSENT" },
            inT = obj.optString("inT", "-").ifBlank { "-" },
            outT = obj.optString("outT", "-").ifBlank { "-" },
            present = present,
            half = half,
            total = total,
            pct = pct
        )
    }

    private fun parseHolidays(arr: JSONArray?): List<HolidayItem> {
        if (arr == null) return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                add(
                    HolidayItem(
                        date = item.optString("date", "").trim(),
                        title = item.optString("title", item.optString("name", "Holiday")).trim()
                    )
                )
            }
        }
    }

    private fun parseSingleHoliday(obj: JSONObject?): HolidayItem? {
        if (obj == null) return null
        val title = obj.optString("title", obj.optString("name", "")).trim()
        val date = obj.optString("date", "").trim()
        if (title.isBlank() && date.isBlank()) return null
        return HolidayItem(date = date, title = title.ifBlank { "Holiday" })
    }

    private fun parseNotices(arr: JSONArray?): List<NoticeItem> {
        if (arr == null) return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                add(
                    NoticeItem(
                        id = item.optString("id", i.toString()).trim(),
                        title = item.optString("title", "Notice").trim(),
                        body = item.optString("body", item.optString("message", "")).trim(),
                        date = item.optString("date", "").trim(),
                        isPinned = item.optBoolean("isPinned", item.optBoolean("pinned", false))
                    )
                )
            }
        }
    }

    private fun parseSingleNoticeOrNull(obj: JSONObject?): NoticeItem? {
        if (obj == null) return null
        val title = obj.optString("title", "").trim()
        val body = obj.optString("body", obj.optString("message", "")).trim()
        if (title.isBlank() && body.isBlank()) return null
        return NoticeItem(
            id = obj.optString("id", "pinned").trim(),
            title = title.ifBlank { "Announcement" },
            body = body,
            date = obj.optString("date", "").trim(),
            isPinned = true
        )
    }

    private fun parseShopFloorList(arr: JSONArray?): List<ShopFloorEntry> {
        if (arr == null) return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                add(
                    ShopFloorEntry(
                        id = item.optString("id", "").trim(),
                        date = item.optString("date", "").trim(),
                        roll = item.optSafeString("roll"),
                        name = item.optString("name", "Trainee").trim(),
                        attendancePct = item.optSafeInt("attendancePct"),
                        sessionType = item.optString("sessionType", "POST_3PM").trim(),
                        scheduledEndTime = item.optString("scheduledEndTime", "05:00 PM").trim(),
                        assignedReason = item.optString("assignedReason", "").trim(),
                        status = item.optString("status", "SCHEDULED").trim()
                    )
                )
            }
        }
    }

    private fun parseSingleShopFloorOrNull(obj: JSONObject?): ShopFloorEntry? {
        if (obj == null) return null
        val roll = obj.optSafeString("roll")
        if (roll.isBlank()) return null
        return ShopFloorEntry(
            id = obj.optString("id", "").trim(),
            date = obj.optString("date", "").trim(),
            roll = roll,
            name = obj.optString("name", "Trainee").trim(),
            attendancePct = obj.optSafeInt("attendancePct"),
            sessionType = obj.optString("sessionType", "POST_3PM").trim(),
            scheduledEndTime = obj.optString("scheduledEndTime", "05:00 PM").trim(),
            assignedReason = obj.optString("assignedReason", "").trim(),
            status = obj.optString("status", "SCHEDULED").trim()
        )
    }

    private fun mergeAutoFlaggedShopFloor(
        attendanceList: List<StaffAttendanceRecord>,
        serverList: List<ShopFloorEntry>,
        date: String
    ): List<ShopFloorEntry> {
        val byRoll = linkedMapOf<String, ShopFloorEntry>()
        serverList.forEach { byRoll[it.roll] = it }
        attendanceList.filter { it.pct < AttendanceMath.EXAM_THRESHOLD_PCT && it.total > 0 }.forEach { rec ->
            if (!byRoll.containsKey(rec.roll)) {
                byRoll[rec.roll] = ShopFloorEntry(
                    id = "AUTO-${rec.roll}",
                    date = date,
                    roll = rec.roll,
                    name = rec.name,
                    attendancePct = rec.pct,
                    sessionType = "POST_3PM",
                    scheduledEndTime = "05:00 PM",
                    assignedReason = "Auto-flagged (<80% attendance)",
                    status = "SCHEDULED"
                )
            }
        }
        return byRoll.values.toList()
    }

    private fun parseScanOverrides(obj: JSONObject?): ScanOverrideState {
        if (obj == null) return ScanOverrideState()
        return ScanOverrideState(
            earlyClassLogoutEnabled = obj.optBoolean("earlyClassLogoutEnabled", obj.optBoolean("allowEarlyLogout", false)),
            shopFloorReleaseNowEnabled = obj.optBoolean("shopFloorReleaseNowEnabled", obj.optBoolean("releaseShopFloorNow", false))
        )
    }

    private fun parseToolCribLogs(arr: JSONArray?): List<ToolCribLogItem> {
        if (arr == null) return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                add(
                    ToolCribLogItem(
                        id = item.optString("id", "").trim(),
                        roll = item.optSafeString("roll"),
                        name = item.optString("name", "").trim(),
                        tool = item.optString("tool", item.optString("item", "")).trim(),
                        status = item.optString("status", "").trim(),
                        time = item.optString("time", "").trim()
                    )
                )
            }
        }
    }

    private fun parseDutySignOff(obj: JSONObject?): DutySignOffRecord? {
        if (obj == null) return null
        val signedOff = obj.optBoolean("signedOff", false)
        val signedAt = obj.optString("signedAt", "").trim()
        if (!signedOff && signedAt.isBlank()) return null
        return DutySignOffRecord(
            date = obj.optString("date", "").trim(),
            pairLabel = obj.optString("pairLabel", "").trim(),
            signedOff = signedOff,
            signedAt = signedAt,
            note = obj.optString("note", "").trim()
        )
    }

    private fun parseParentAlerts(arr: JSONArray?): List<ParentAlertItem> {
        if (arr == null) return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                add(
                    ParentAlertItem(
                        id = item.optString("id", item.optString("alertId", "")).trim(),
                        roll = item.optSafeString("roll"),
                        studentName = item.optString("studentName", item.optString("name", "")).trim(),
                        alertType = item.optString("alertType", item.optString("type", "")).trim(),
                        message = item.optString("message", item.optString("reason", "")).trim(),
                        date = item.optString("date", "").trim(),
                        status = item.optString("status", "SENT").trim()
                    )
                )
            }
        }
    }

    private fun parseProjectGroups(arr: JSONArray?): List<ProjectGroupItem> {
        if (arr == null) return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                add(
                    ProjectGroupItem(
                        id = item.optString("id", "").trim(),
                        title = item.optString("title", "").trim(),
                        members = item.optString("members", "").trim(),
                        status = item.optString("status", "").trim()
                    )
                )
            }
        }
    }

    private fun parseStringList(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val v = arr.optString(i, "").trim()
                if (v.isNotBlank()) add(v)
            }
        }
    }

    private fun JSONObject.optAttendanceClosed(): Boolean {
        return optBoolean("attendanceClosed", optBoolean("isClosed", false))
    }

    private fun JSONObject.optSafeString(key: String, fallback: String = ""): String {
        if (!has(key) || isNull(key)) return fallback
        val raw = opt(key) ?: return fallback
        return when (raw) {
            is Number -> {
                val d = raw.toDouble()
                if (d == d.toLong().toDouble()) d.toLong().toString() else raw.toString()
            }
            else -> raw.toString().trim().ifBlank { fallback }
        }
    }

    private fun JSONObject.optSafeInt(key: String): Int {
        if (!has(key) || isNull(key)) return 0
        val raw = opt(key) ?: return 0
        return when (raw) {
            is Number -> raw.toDouble().roundToInt()
            else -> raw.toString().trim().toDoubleOrNull()?.roundToInt() ?: 0
        }
    }
}
