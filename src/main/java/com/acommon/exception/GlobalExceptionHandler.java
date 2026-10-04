package com.acommon.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        log.warn("Échec d'authentification sur [{}]: {}", request.getRequestURI(), ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
        errorResponse.setError(HttpStatus.UNAUTHORIZED.getReasonPhrase());
        errorResponse.setMessage(ex.getMessage() != null && !ex.getMessage().isBlank() ? ex.getMessage() : "Identifiant ou mot de passe incorrect");
        errorResponse.setErrorCode("BAD_CREDENTIALS");
        errorResponse.setPath(request.getRequestURI());
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccountStatusException.class)
    public ResponseEntity<ErrorResponse> handleAccountStatusException(AccountStatusException ex, HttpServletRequest request) {
        log.warn("Compte inactif sur [{}]: {}", request.getRequestURI(), ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setStatus(HttpStatus.FORBIDDEN.value());
        errorResponse.setError(HttpStatus.FORBIDDEN.getReasonPhrase());
        errorResponse.setMessage(ex.getMessage());
        errorResponse.setErrorCode("ACCOUNT_DISABLED");
        errorResponse.setPath(request.getRequestURI());
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Accès refusé sur [{}]: {}", request.getRequestURI(), ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setStatus(HttpStatus.FORBIDDEN.value());
        errorResponse.setError(HttpStatus.FORBIDDEN.getReasonPhrase());
        errorResponse.setMessage("Accès refusé : vous n'avez pas les autorisations nécessaires");
        errorResponse.setErrorCode("ACCESS_DENIED");
        errorResponse.setPath(request.getRequestURI());
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(CommonException.class)
    public ResponseEntity<ErrorResponse> handlepointventException(CommonException ex, HttpServletRequest request) {
        log.warn("Erreur métier [{}] sur [{}]: {}", ex.getErrorCode(), request.getRequestURI(), ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setStatus(ex.getStatus().value());
        errorResponse.setError(ex.getStatus().getReasonPhrase());
        errorResponse.setMessage(ex.getMessage());
        errorResponse.setErrorCode(ex.getErrorCode());
        errorResponse.setPath(request.getRequestURI());
        return new ResponseEntity<>(errorResponse, ex.getStatus());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Ressource non trouvée sur [{}]: {}", request.getRequestURI(), ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setStatus(HttpStatus.NOT_FOUND.value());
        errorResponse.setError(HttpStatus.NOT_FOUND.getReasonPhrase());
        errorResponse.setMessage(ex.getMessage() != null ? ex.getMessage() : "Ressource non trouvée");
        errorResponse.setErrorCode("NOT_FOUND");
        errorResponse.setPath(request.getRequestURI());
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ErrorResponse> handleBadRequestExceptions(RuntimeException ex, HttpServletRequest request) {
        log.warn("Requête invalide sur [{}]: {}", request.getRequestURI(), ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setStatus(HttpStatus.BAD_REQUEST.value());
        errorResponse.setError(HttpStatus.BAD_REQUEST.getReasonPhrase());
        errorResponse.setMessage(ex.getMessage() != null && !ex.getMessage().isBlank() ? ex.getMessage() : "Requête invalide");
        errorResponse.setErrorCode("BAD_REQUEST");
        errorResponse.setPath(request.getRequestURI());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(java.io.IOException.class)
    public ResponseEntity<ErrorResponse> handleIOException(java.io.IOException ex, HttpServletRequest request) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "";
        if (msg.contains("abandonnée") || msg.toLowerCase().contains("broken pipe") || msg.toLowerCase().contains("connection reset")
                || ex instanceof org.apache.catalina.connector.ClientAbortException) {
            log.debug("Connexion interrompue par le client sur [{}] : {}", request.getRequestURI(), msg);
            return null;
        }
        log.error("Erreur d'entrée/sortie sur [{}] : {}", request.getRequestURI(), ex.getMessage(), ex);
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        errorResponse.setError(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
        errorResponse.setMessage("Erreur d'entrée/sortie réseau");
        errorResponse.setErrorCode("IO_ERROR");
        errorResponse.setPath(request.getRequestURI());
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(org.springframework.dao.DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Violation d'intégrité de données sur [{}]: {}", request.getRequestURI(), ex.getMessage());
        return buildDatabaseErrorResponse(ex, request);
    }

    @ExceptionHandler(java.sql.SQLException.class)
    public ResponseEntity<ErrorResponse> handleSQLException(java.sql.SQLException ex, HttpServletRequest request) {
        log.error("Erreur SQL interceptée sur [{}]: {}", request.getRequestURI(), ex.getMessage(), ex);
        return buildDatabaseErrorResponse(ex, request);
    }

    @ExceptionHandler(org.springframework.transaction.TransactionSystemException.class)
    public ResponseEntity<ErrorResponse> handleTransactionSystemException(org.springframework.transaction.TransactionSystemException ex, HttpServletRequest request) {
        log.error("Erreur de transaction sur [{}]: {}", request.getRequestURI(), ex.getMessage(), ex);
        Throwable root = getRootCause(ex);
        if (root instanceof org.springframework.dao.DataIntegrityViolationException || root instanceof java.sql.SQLException || isSqlRelated(root != null ? root.getMessage() : null)) {
            return buildDatabaseErrorResponse(root != null ? root : ex, request);
        }
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        errorResponse.setError(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
        errorResponse.setMessage("Une erreur est survenue lors de l'enregistrement de l'opération.");
        errorResponse.setErrorCode("TRANSACTION_ERROR");
        errorResponse.setPath(request.getRequestURI());
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex, HttpServletRequest request) {
        log.error("Erreur serveur inattendue sur [{}] : {}", request.getRequestURI(), ex.getMessage(), ex);

        Throwable root = getRootCause(ex);
        String raw = (root != null && root.getMessage() != null) ? root.getMessage() : (ex.getMessage() != null ? ex.getMessage() : "");

        if (isSqlRelated(raw)) {
            return buildDatabaseErrorResponse(root != null ? root : ex, request);
        }

        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        errorResponse.setError(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());

        // Masquage absolu de tout détail technique interne ou SQL vers le front
        String cleanMessage = "Une erreur inattendue s'est produite lors du traitement.";
        if (ex.getMessage() != null && !ex.getMessage().isBlank() && !isInternalTechnicalMessage(ex.getMessage())) {
            cleanMessage = ex.getMessage();
        }

        errorResponse.setMessage(cleanMessage);
        errorResponse.setErrorCode("INTERNAL_ERROR");
        errorResponse.setPath(request.getRequestURI());
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ErrorResponse> buildDatabaseErrorResponse(Throwable ex, HttpServletRequest request) {
        Throwable root = getRootCause(ex);
        String raw = (root != null && root.getMessage() != null) ? root.getMessage().toLowerCase() : (ex.getMessage() != null ? ex.getMessage().toLowerCase() : "");

        String userFriendlyMessage;
        String errorCode;
        HttpStatus status = HttpStatus.CONFLICT;

        if (raw.contains("uk_produits_tenant_reference") || raw.contains("uk_rj4mr27ga20ughn6qq6ev2uh0") || (raw.contains("reference") && raw.contains("duplicate key"))) {
            userFriendlyMessage = "Cette référence produit est déjà attribuée pour cette société. Veuillez saisir une référence unique ou laisser le champ vide.";
            errorCode = "DUPLICATE_PRODUCT_REFERENCE";
        } else if (raw.contains("uk_factures_tenant_numero") || (raw.contains("numero_facture") && raw.contains("duplicate key"))) {
            userFriendlyMessage = "Ce numéro de facture existe déjà pour cette société.";
            errorCode = "DUPLICATE_FACTURE_NUMBER";
        } else if (raw.contains("uk_commandes_tenant_numero") || raw.contains("uk_commandes_client_tenant_numero") || (raw.contains("numero_commande") && raw.contains("duplicate key"))) {
            userFriendlyMessage = "Ce numéro de commande existe déjà pour cette société.";
            errorCode = "DUPLICATE_COMMANDE_NUMBER";
        } else if (raw.contains("uk_livraisons_tenant_numero") || raw.contains("uk_bl_client_tenant_numero") || (raw.contains("numero_bl") && raw.contains("duplicate key"))) {
            userFriendlyMessage = "Ce numéro de bon de livraison existe déjà pour cette société.";
            errorCode = "DUPLICATE_LIVRAISON_NUMBER";
        } else if (raw.contains("uk_devis_tenant_numero") || (raw.contains("numero_devis") && raw.contains("duplicate key"))) {
            userFriendlyMessage = "Ce numéro de devis existe déjà pour cette société.";
            errorCode = "DUPLICATE_DEVIS_NUMBER";
        } else if (raw.contains("code_barre") && raw.contains("duplicate key")) {
            userFriendlyMessage = "Ce code-barres est déjà utilisé pour un autre article.";
            errorCode = "DUPLICATE_BARCODE";
        } else if (raw.contains("duplicate key") || raw.contains("unique constraint") || raw.contains("déjà") || raw.contains("unique")) {
            userFriendlyMessage = "Un enregistrement avec ces informations existe déjà (doublon détecté).";
            errorCode = "DUPLICATE_ENTRY";
        } else if (raw.contains("foreign key") || raw.contains("violates foreign key") || raw.contains("still referenced") || raw.contains("is referenced from")) {
            userFriendlyMessage = "Impossible d'effectuer cette opération car cet élément est lié à d'autres données (ex. commandes, factures, mouvements).";
            errorCode = "FOREIGN_KEY_VIOLATION";
            status = HttpStatus.BAD_REQUEST;
        } else if (raw.contains("not-null") || raw.contains("null value in column")) {
            userFriendlyMessage = "Un champ obligatoire n'a pas été renseigné.";
            errorCode = "REQUIRED_FIELD_MISSING";
            status = HttpStatus.BAD_REQUEST;
        } else if (raw.contains("check constraint") || raw.contains("violates check")) {
            userFriendlyMessage = "Une des valeurs saisies ne respecte pas les règles requises.";
            errorCode = "CHECK_CONSTRAINT_VIOLATION";
            status = HttpStatus.BAD_REQUEST;
        } else {
            userFriendlyMessage = "Une erreur est survenue lors de l'enregistrement en base de données.";
            errorCode = "DATABASE_OPERATION_FAILED";
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setStatus(status.value());
        errorResponse.setError(status.getReasonPhrase());
        errorResponse.setMessage(userFriendlyMessage);
        errorResponse.setErrorCode(errorCode);
        errorResponse.setPath(request.getRequestURI());
        return new ResponseEntity<>(errorResponse, status);
    }

    private Throwable getRootCause(Throwable throwable) {
        if (throwable == null) return null;
        Throwable root = throwable;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root;
    }

    private boolean isSqlRelated(String msg) {
        if (msg == null) return false;
        String m = msg.toLowerCase();
        return m.contains("sql") || m.contains("statement") || m.contains("duplicate key")
                || m.contains("constraint") || m.contains("table") || m.contains("column")
                || m.contains("hibernate") || m.contains("psqlexception") || m.contains("violates")
                || m.contains("insert into") || m.contains("update ") || m.contains("delete from")
                || m.contains("select ") || m.contains("relation ");
    }

    private boolean isInternalTechnicalMessage(String msg) {
        if (msg == null) return false;
        String m = msg.toLowerCase();
        return isSqlRelated(msg) || m.contains("exception") || m.contains("nullpointer")
                || m.contains("stacktrace") || m.contains("class") || m.contains(".java");
    }

    @Override
    protected ResponseEntity<Object> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex,
                                                                 HttpHeaders headers,
                                                                 HttpStatusCode status,
                                                                 WebRequest request) {
        log.warn("Upload trop volumineux sur [{}] : {}", request.getContextPath(), ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setStatus(HttpStatus.PAYLOAD_TOO_LARGE.value());
        errorResponse.setError(HttpStatus.PAYLOAD_TOO_LARGE.getReasonPhrase());
        errorResponse.setMessage("Fichier trop volumineux. Taille maximale autorisée.");
        errorResponse.setErrorCode("UPLOAD_SIZE_EXCEEDED");
        errorResponse.setPath(request.getContextPath());
        return new ResponseEntity<>(errorResponse, HttpStatus.PAYLOAD_TOO_LARGE);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        log.warn("Validation échouée sur [{}]: {} erreurs", request.getContextPath(), ex.getBindingResult().getErrorCount());
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setStatus(HttpStatus.BAD_REQUEST.value());
        errorResponse.setError(HttpStatus.BAD_REQUEST.getReasonPhrase());
        errorResponse.setMessage("Erreur de validation");
        errorResponse.setErrorCode("VALIDATION_ERROR");
        errorResponse.setPath(request.getContextPath());

        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errorResponse.addValidationError(fieldError.getField(), fieldError.getDefaultMessage());
        }

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
}