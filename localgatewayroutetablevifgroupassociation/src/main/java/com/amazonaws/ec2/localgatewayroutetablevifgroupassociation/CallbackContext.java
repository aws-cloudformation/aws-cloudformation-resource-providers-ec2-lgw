package com.amazonaws.ec2.localgatewayroutetablevifgroupassociation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
// Need these constructors for deserialization to work properly
@NoArgsConstructor
@AllArgsConstructor
public class CallbackContext {
    private boolean createStarted;
    private boolean deleteStarted;
    private boolean updateStarted;

    private Set<Tag> tagsToCreate;
    private Set<Tag> tagsToDelete;

    public static final int POLLING_DELAY_SECONDS = 5;
}