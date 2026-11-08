package com.chatapp.chat_backend.dto.request;

import com.chatapp.chat_backend.model.privacy.WhoCanAddToGroups;
import com.chatapp.chat_backend.model.privacy.WhoCanMessage;

public record UpdatePrivacySettingsRequest(
                WhoCanMessage whoCanMessage,
                WhoCanAddToGroups whoCanAddToGroups) {
}
