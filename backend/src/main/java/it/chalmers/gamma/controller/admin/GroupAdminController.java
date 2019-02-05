package it.chalmers.gamma.controller.admin;

import it.chalmers.gamma.db.entity.FKITGroup;
import it.chalmers.gamma.db.entity.FKITSuperGroup;
import it.chalmers.gamma.db.entity.Website;
import it.chalmers.gamma.db.entity.WebsiteInterface;
import it.chalmers.gamma.db.entity.WebsiteURL;

import it.chalmers.gamma.requests.CreateGroupRequest;
import it.chalmers.gamma.response.GroupAlreadyExistsResponse;
import it.chalmers.gamma.response.GroupCreatedResponse;
import it.chalmers.gamma.response.GroupDeletedResponse;
import it.chalmers.gamma.response.GroupDoesNotExistResponse;
import it.chalmers.gamma.response.GroupEditedResponse;
import it.chalmers.gamma.response.GroupsResponse;
import it.chalmers.gamma.response.InputValidationFailedResponse;
import it.chalmers.gamma.service.FKITService;
import it.chalmers.gamma.service.FKITSuperGroupService;
import it.chalmers.gamma.service.GroupWebsiteService;

import it.chalmers.gamma.service.WebsiteService;

import it.chalmers.gamma.util.InputValidationUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@SuppressWarnings({"PMD.ExcessiveImports", "PMD.AvoidDuplicateLiterals"})
@RestController
@RequestMapping("/admin/groups")
public final class GroupAdminController {

    private final FKITService fkitService;
    private final WebsiteService websiteService;
    private final GroupWebsiteService groupWebsiteService;
    private final FKITSuperGroupService fkitSuperGroupService;

    public GroupAdminController(
            FKITService fkitService,
            WebsiteService websiteService,
            GroupWebsiteService groupWebsiteService,
            FKITSuperGroupService fkitSuperGroupService) {
        this.fkitService = fkitService;
        this.websiteService = websiteService;
        this.groupWebsiteService = groupWebsiteService;
        this.fkitSuperGroupService = fkitSuperGroupService;
    }

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<List<FKITGroup>> getGroups() {
        return new GroupsResponse(this.fkitService.getGroups());
    }

    @SuppressWarnings("PMD.CyclomaticComplexity")
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<String> addNewGroup(@Valid @RequestBody CreateGroupRequest createGroupRequest,
                                              BindingResult result) {
        if (this.fkitService.groupExists(createGroupRequest.getName())) {
            throw new GroupAlreadyExistsResponse();
        }

        if (result.hasErrors()) {
            throw new InputValidationFailedResponse(InputValidationUtils.getErrorMessages(result.getAllErrors()));
        }
        FKITSuperGroup superGroup = this.fkitSuperGroupService.getGroup(
                UUID.fromString(createGroupRequest.getSuperGroup()));

        if (superGroup == null) {
            throw new GroupDoesNotExistResponse();
        }

        List<CreateGroupRequest.WebsiteInfo> websites = createGroupRequest.getWebsites();
        if (websites == null || websites.isEmpty()) {
            return new GroupCreatedResponse();
        }
        List<WebsiteURL> websiteURLs = new ArrayList<>();
        for (CreateGroupRequest.WebsiteInfo websiteInfo : websites) {
            Website website = this.websiteService.getWebsite(websiteInfo.getWebsite());
            WebsiteURL websiteURL = new WebsiteURL();
            websiteURL.setWebsite(website);
            websiteURL.setUrl(websiteInfo.getUrl());
            websiteURLs.add(websiteURL);
        }
        this.groupWebsiteService.addGroupWebsites(
                this.fkitService.createGroup(createGroupRequest, superGroup), websiteURLs);
        return new GroupCreatedResponse();
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    public ResponseEntity<String> editGroup(
            @RequestBody CreateGroupRequest request,
            @PathVariable("id") String id) {
        if (!this.fkitService.groupExists(UUID.fromString(id))) {
            throw new GroupDoesNotExistResponse();
        }
        this.fkitService.editGroup(UUID.fromString(id), request);
        FKITGroup group = this.fkitService.getGroup(UUID.fromString(id));
        List<CreateGroupRequest.WebsiteInfo> websiteInfos = request.getWebsites();
        List<WebsiteInterface> entityWebsites = new ArrayList<>(
                this.groupWebsiteService.getWebsites(group)
        );
        List<WebsiteURL> websiteURLs = this.groupWebsiteService.addWebsiteToEntity(
                websiteInfos, entityWebsites
        );
        this.groupWebsiteService.addGroupWebsites(group, websiteURLs);
        return new GroupEditedResponse();
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<String> deleteGroup(@PathVariable("id") String id) {
        if (!this.fkitService.groupExists(UUID.fromString(id))) {
            throw new GroupDoesNotExistResponse();
        }
        this.groupWebsiteService.deleteWebsitesConnectedToGroup(
                this.fkitService.getGroup(UUID.fromString(id))
        );
        this.fkitService.removeGroup(UUID.fromString(id));
        return new GroupDeletedResponse();
    }

}