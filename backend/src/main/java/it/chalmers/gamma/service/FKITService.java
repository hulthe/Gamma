package it.chalmers.gamma.service;

import it.chalmers.gamma.db.entity.FKITGroup;
import it.chalmers.gamma.db.entity.Text;
import it.chalmers.gamma.db.repository.FKITGroupRepository;
import it.chalmers.gamma.db.repository.TextRepository;
import it.chalmers.gamma.domain.GroupType;
import org.springframework.stereotype.Service;

@Service
public class FKITService {

    private final FKITGroupRepository repo;

    private final TextRepository textRepository;

    public FKITService(FKITGroupRepository repo, TextRepository textRepository) {
        this.repo = repo;
        this.textRepository = textRepository;
    }

    public FKITGroup createGroup(String name, Text description, String email, GroupType type, Text function) {
        FKITGroup fkitGroup = new FKITGroup();
        fkitGroup.setName(name.toLowerCase());
        return saveGroup(fkitGroup, name, description, email, type, function);
    }

    public FKITGroup editGroup(String name, Text description, String email, GroupType type, Text function){ //TODO if no info, don't change value.
        FKITGroup group = repo.findByName(name.toLowerCase());
        group.setSVFunction(function.getSv());
        group.setENFunction(function.getEn());
        function = group.getFunction();
        if(description != null) {
            if (group.getDescription() != null) {
                group.setSVDescription(description.getSv());
                group.setENDescription(description.getEn());
            }
        }
        return saveGroup(group, name, description, email, type, function);
    }

    private FKITGroup saveGroup(FKITGroup fkitGroup, String name, Text description,
                                String email, GroupType type, Text function){
        fkitGroup.setPrettyName(name);
        fkitGroup.setDescription(description);
        fkitGroup.setEmail(email);
        fkitGroup.setType(type);
        fkitGroup.setFunction(function);
        return repo.save(fkitGroup);
    }
    public boolean groupExists(String name){
        return repo.existsFKITGroupByName(name);
    }
    public FKITGroup getGroup(String group){
        return repo.findByName(group);
    }
    public void removeGroup(String group){
        repo.delete(repo.findByName(group));
    }
}
