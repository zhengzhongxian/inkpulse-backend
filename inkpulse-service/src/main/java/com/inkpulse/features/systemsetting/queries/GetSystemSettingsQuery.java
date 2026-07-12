package com.inkpulse.features.systemsetting.queries;

import com.inkpulse.cqrs.Query;
import com.inkpulse.models.response.systemsetting.SystemSettingResponse;
import java.util.List;

public class GetSystemSettingsQuery implements Query<List<SystemSettingResponse>> {
}
