package at.martinahrer.cd;

import at.martinahrer.cd.model.Company;
import org.springframework.data.repository.PagingAndSortingRepository;


public interface CompanyRepository extends PagingAndSortingRepository<Company, Long> {
}
