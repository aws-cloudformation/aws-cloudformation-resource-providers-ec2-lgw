package com.amazonaws.ec2.localgatewayroute;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
// Need these constructors for deserialization to work properly
@NoArgsConstructor
@AllArgsConstructor
public class CallbackContext {
    private boolean createStarted;
    private boolean deleteStarted;
    public static final int POLLING_DELAY_SECONDS = 5;
}
