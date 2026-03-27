package kkkvd.docflow.repositories;

import kkkvd.docflow.entities.Attachment;
import kkkvd.docflow.entities.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    List<Attachment> findByDocument(Document document);
}
