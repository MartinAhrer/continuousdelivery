package at.martinahrer.cd;

import at.martinahrer.cd.model.Company;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * Created by martin on 19/03/16.
 */
public interface CompanyRepository extends PagingAndSortingRepository<Company, Long> {
}
