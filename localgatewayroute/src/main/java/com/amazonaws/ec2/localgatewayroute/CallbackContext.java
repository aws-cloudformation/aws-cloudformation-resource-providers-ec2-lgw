package com.amazonaws.ec2.localgatewayroute;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CallbackContext {
    private boolean createStarted;
    private boolean deleteStarted;
    public static final int POLLING_DELAY_SECONDS = 5;
}
