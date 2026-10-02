package com.gestion.ai.security;

import com.acommon.persistant.model.User;
import java.util.Set;

public class AiTenantScope {

    private final User user;
    private final Long tenantId;
    private final Long mereId;
    private final boolean holding;
    private final Set<Long> accessibleTenantIds;
    private final String role;
    private final String nomEntreprise;

    public AiTenantScope(User user,
                         Long tenantId,
                         Long mereId,
                         boolean holding,
                         Set<Long> accessibleTenantIds,
                         String role,
                         String nomEntreprise) {
        this.user = user;
        this.tenantId = tenantId;
        this.mereId = mereId;
        this.holding = holding;
        this.accessibleTenantIds = accessibleTenantIds;
        this.role = role;
        this.nomEntreprise = nomEntreprise;
    }

    public User getUser() {
        return user;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public Long getMereId() {
        return mereId;
    }

    public boolean isHolding() {
        return holding;
    }

    public Set<Long> getAccessibleTenantIds() {
        return accessibleTenantIds;
    }

    public String getRole() {
        return role;
    }

    public String getNomEntreprise() {
        return nomEntreprise;
    }
}
