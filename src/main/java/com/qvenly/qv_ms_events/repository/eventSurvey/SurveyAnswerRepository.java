package com.qvenly.qv_ms_events.repository.eventSurvey;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.qvenly.qv_ms_events.model.entity.eventSurvey.SurveyAnswer;

public interface SurveyAnswerRepository extends JpaRepository<SurveyAnswer, Long> {

    List<SurveyAnswer> findByResponseId(Long responseId);
 
    /**
     * Cuenta cuántas veces fue seleccionada cada opción por pregunta —
     * base para las estadísticas de preguntas cerradas (RF136)
     */
    @Query("""
            SELECT a.selectedOption.id, COUNT(a)
            FROM SurveyAnswer a
            WHERE a.question.id = :questionId
            AND a.selectedOption IS NOT NULL
            GROUP BY a.selectedOption.id
            """)
    List<Object[]> countByOptionForQuestion(@Param("questionId") Long questionId);
 
    /**
     * Trae todas las respuestas abiertas de una pregunta — para mostrar
     * los comentarios libres en los resultados (RF135, RF136)
     */
    @Query("""
            SELECT a.openAnswer FROM SurveyAnswer a
            WHERE a.question.id = :questionId
            AND a.openAnswer IS NOT NULL
            AND a.openAnswer <> ''
            """)
    List<String> findOpenAnswersByQuestion(@Param("questionId") Long questionId);
}
