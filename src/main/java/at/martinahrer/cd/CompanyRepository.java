package at.martinahrer.cd;

import at.martinahrer.cd.model.Company;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;


@RepositoryRestResource
public interface CompanyRepository extends PagingAndSortingRepository<Company, Long> {
}

