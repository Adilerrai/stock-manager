package com.gestion.service.compta;

import com.acommon.exception.CommonException;
import com.gestion.persistent.dto.compta.EtatDtos.ParametreFiscalDTO;
import com.gestion.repository.compta.ParametreFiscalRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/** Les taux et seuils datés, avec leur source. Pas de valeur à la date, pas de calcul : aucun taux par défaut. */
@Service
public class ParametreFiscalService {

    private final ParametreFiscalRepository repository;

    public ParametreFiscalService(ParametreFiscalRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public ParametreFiscalDTO aLaDate(String code, String cle, LocalDate date) {
        return repository.aLaDate(code, cle == null ? "" : cle, date).orElseThrow(() -> new CommonException(
                "Aucune valeur de " + code + (cle == null || cle.isEmpty() ? "" : " (" + cle + ")") + " au " + date + " : le calcul ne peut pas se faire.",
                HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_PARAMETRE_ABSENT"));
    }

    @Transactional(readOnly = true)
    public List<ParametreFiscalDTO> lister(String code) {
        return repository.lister(code);
    }
}
