package com.acommon.annotation;


import com.acommon.persistant.model.TenantContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
public class MultitenantSearchAspect {

    private static final Logger logger = LoggerFactory.getLogger(MultitenantSearchAspect.class);

    @Around("@annotation(com.acommon.annotation.MultitenantSearchMethod)")
    public Object aroundMultitenantSearchMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        MultitenantSearchMethod annotation = method.getAnnotation(MultitenantSearchMethod.class);

        Long tenantId = TenantContext.getCurrentTenant();

        if (tenantId == null && annotation.failIfNoTenant()) {
            throw new IllegalStateException("Aucun tenant trouvé dans le contexte pour la méthode " + method.getName());
        }

        logger.debug("Exécution de la méthode multi-tenant {} avec tenant ID: {}",
                method.getName(), tenantId != null ? tenantId : "AUCUN");



        return joinPoint.proceed();
    }
}