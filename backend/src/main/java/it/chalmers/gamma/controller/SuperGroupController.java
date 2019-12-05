package it.chalmers.gamma.controller;

import it.chalmers.gamma.domain.dto.group.FKITGroupToSuperGroupDTO;
import it.chalmers.gamma.domain.dto.group.FKITSuperGroupDTO;
import it.chalmers.gamma.response.group.GetActiveFKITGroupsResponse;
import it.chalmers.gamma.response.group.GetActiveFKITGroupsResponse.GetActiveFKITGroupResponseObject;
import it.chalmers.gamma.response.group.GetAllFKITGroupsMinifiedResponse;
import it.chalmers.gamma.response.group.GetAllFKITGroupsMinifiedResponse.GetAllFKITGroupsMinifiedResponseObject;
import it.chalmers.gamma.response.group.GetFKITGroupMinifiedResponse;
import it.chalmers.gamma.response.group.GetFKITGroupResponse;
import it.chalmers.gamma.response.super_group.GetAllSuperGroupsResponse;
import it.chalmers.gamma.response.super_group.GetAllSuperGroupsResponse.GetAllSuperGroupsResponseObject;
import it.chalmers.gamma.response.super_group.GetSuperGroupResponse;
import it.chalmers.gamma.response.group.GroupDoesNotExistResponse;
import it.chalmers.gamma.service.FKITGroupToSuperGroupService;
import it.chalmers.gamma.service.FKITSuperGroupService;
import it.chalmers.gamma.service.GroupWebsiteService;
import it.chalmers.gamma.service.MembershipService;

import java.util.ArrayList;
import java.util.List;

import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/superGroups")
public class SuperGroupController {

    private final FKITSuperGroupService fkitSuperGroupService;
    private final FKITGroupToSuperGroupService fkitGroupToSuperGroupService;
    private final GroupWebsiteService groupWebsiteService;
    private final MembershipService membershipService;

    public SuperGroupController(FKITSuperGroupService fkitSuperGroupService,
                                FKITGroupToSuperGroupService fkitGroupToSuperGroupService,
                                GroupWebsiteService groupWebsiteService,
                                MembershipService membershipService) {
        this.fkitSuperGroupService = fkitSuperGroupService;
        this.fkitGroupToSuperGroupService = fkitGroupToSuperGroupService;
        this.groupWebsiteService = groupWebsiteService;
        this.membershipService = membershipService;
    }

    @RequestMapping(value = "/{id}/subgroups", method = RequestMethod.GET)
    public GetAllFKITGroupsMinifiedResponseObject getAllSubGroups(@PathVariable("id") String id) {
        FKITSuperGroupDTO superGroup = this.fkitSuperGroupService.getGroupDTO(id);
        List<FKITGroupToSuperGroupDTO> groupRelationships =
                this.fkitGroupToSuperGroupService.getRelationships(superGroup);
        List<GetFKITGroupMinifiedResponse> responses = groupRelationships.stream().map(
                g -> new GetFKITGroupMinifiedResponse(g.getGroup().toMinifiedDTO())).collect(Collectors.toList());
        return new GetAllFKITGroupsMinifiedResponse(responses).toResponseObject();
    }

    @RequestMapping(method = RequestMethod.GET)
    public GetAllSuperGroupsResponseObject getAllSuperGroups() {
        return new GetAllSuperGroupsResponse(this.fkitSuperGroupService.getAllGroups()
                .stream().map(GetSuperGroupResponse::new).collect(Collectors.toList())).toResponseObject();
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public GetSuperGroupResponse getSuperGroup(@PathVariable("id") String id) {
        if (!this.fkitSuperGroupService.groupExists(id)) {
            throw new GroupDoesNotExistResponse();
        }
        return new GetSuperGroupResponse(this.fkitSuperGroupService.getGroupDTO(id));
    }

    @RequestMapping(value = "/{id}/active", method = RequestMethod.GET)
    public GetActiveFKITGroupResponseObject getActiveGroup(@PathVariable("id") String id) {
        FKITSuperGroupDTO superGroup = this.fkitSuperGroupService.getGroupDTO(id);
        List<GetFKITGroupResponse> groups = this.fkitGroupToSuperGroupService.getActiveGroups(superGroup)
                .stream().map(g -> new GetFKITGroupResponse(
                        g,
                        this.membershipService.getMembershipsInGroup(g)))
                .collect(Collectors.toList());
        return new GetActiveFKITGroupsResponse(groups).toResponseObject();
    }
}
