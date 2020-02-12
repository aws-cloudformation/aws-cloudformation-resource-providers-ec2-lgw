package com.amazonaws.ec2.localgatewayroutetablevpcassociation;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CallbackContext {
    private boolean createStarted;
    private boolean deleteStarted;
    private boolean updateStarted;
}
