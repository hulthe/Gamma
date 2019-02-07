package it.chalmers.gamma.service;

import it.chalmers.gamma.db.entity.FKITGroup;
import it.chalmers.gamma.db.entity.FKITGroupToSuperGroup;
import it.chalmers.gamma.db.entity.FKITSuperGroup;
import it.chalmers.gamma.db.entity.pk.FKITGroupToSuperGroupPK;
import it.chalmers.gamma.db.repository.FKITGroupToSuperGroupRepository;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class FKITGroupToSuperGroupService {
    private final FKITGroupToSuperGroupRepository repository;

    public FKITGroupToSuperGroupService(FKITGroupToSuperGroupRepository repository) {
        this.repository = repository;
    }

    public void addRelationship(FKITGroup group, FKITSuperGroup superGroup) {
        FKITGroupToSuperGroupPK id = new FKITGroupToSuperGroupPK(superGroup, group);
        FKITGroupToSuperGroup relationship = new FKITGroupToSuperGroup(id);
        this.repository.save(relationship);
    }

    public List<FKITGroupToSuperGroup> getRelationships(FKITSuperGroup superGroup) {
        return this.repository.findFKITGroupToSuperGroupsById_SuperGroup(superGroup);
    }
    public FKITSuperGroup getSuperGroup(FKITGroup group) {
        return this.repository.findFKITGroupToSuperGroupsById_Group(group).getId().getSuperGroup();
    }
    public void deleteRelationship(FKITGroup group, FKITSuperGroup superGroup) {
        this.repository.delete(this.repository.findFKITGroupToSuperGroupsById_GroupAndId_SuperGroup(group, superGroup));
    }
}