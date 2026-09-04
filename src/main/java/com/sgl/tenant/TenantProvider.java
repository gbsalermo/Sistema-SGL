package com.sgl.tenant;

import java.util.UUID;

import org.springframework.stereotype.Component;

@Component("tenantProvider")
public class TenantProvider {

    public UUID getUnidadeId() {
        return TenantContext.unidadeAtual().orElse(null);
    }
}
