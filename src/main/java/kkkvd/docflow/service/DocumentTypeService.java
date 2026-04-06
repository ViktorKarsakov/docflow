package kkkvd.docflow.service;

import kkkvd.docflow.entities.DocumentType;
import kkkvd.docflow.repositories.DocumentTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Сервис управления типами документов.
@Service
@RequiredArgsConstructor
public class DocumentTypeService {
    private final DocumentTypeRepository documentTypeRepository;

    // Получить только активные типы документов.
    @Transactional(readOnly = true)
    public List<DocumentType> findActive() {
        return documentTypeRepository.findByActiveTrueOrderBySortOrderAscDisplayNameAsc();
    }

    // Получить все типы включая неактивные.
    @Transactional(readOnly = true)
    public List<DocumentType> findAll() {
        return documentTypeRepository.findAll();
    }

    // Получить тип по ID.
    @Transactional(readOnly = true)
    public DocumentType findById(Long id) {
        return documentTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Тип документа не найден: " + id));
    }

    // Создать новый тип документа.
    @Transactional
    public DocumentType create(DocumentType body) {
        documentTypeRepository.findByCode(body.getCode()).ifPresent(existing -> {
            throw new RuntimeException("Тип с кодом '" + body.getCode() + "' уже существует");
        });
        return documentTypeRepository.save(body);
    }

    // Обновить тип документа.
    // Например, изменить название, префикс регистрационного номера или порядок сортировки.
    @Transactional
    public DocumentType update(Long id, DocumentType body) {
        DocumentType dt = documentTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Тип документа не найден: " + id));

        dt.setDisplayName(body.getDisplayName());
        dt.setCode(body.getCode());
        dt.setPrefix(body.getPrefix());
        dt.setDescription(body.getDescription());
        dt.setSortOrder(body.getSortOrder());
        dt.setActive(body.isActive());
        return documentTypeRepository.save(dt);
    }

    // Изменить активность типа документа.
    // Деактивированный тип скрывается из списка при создании документа,
    // но уже созданные документы этого типа остаются.
    @Transactional
    public DocumentType setActive(Long id, boolean active) {
        DocumentType dt = documentTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Тип документа не найден: " + id));
        dt.setActive(active);
        return documentTypeRepository.save(dt);
    }
}
