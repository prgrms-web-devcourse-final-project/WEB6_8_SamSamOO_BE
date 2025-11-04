ALTER TABLE precedent
  ADD FULLTEXT idx_precedent_fulltext (notice, summary_of_the_judgment, precedent_content, case_name, case_number);