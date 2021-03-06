package com.evolveum.midpoint.web.page.self;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.context.*;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.InOidFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorDto;
import com.evolveum.midpoint.web.component.assignment.AssignmentTablePanel;
import com.evolveum.midpoint.web.component.assignment.TargetUserSelectorComponent;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.users.component.ExecuteChangeOptionsDto;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.web.page.self.dto.AssignmentConflictDto;
import com.evolveum.midpoint.web.session.SessionStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Created by honchar.
 */
@PageDescriptor(url = "/self/requestAssignments", encoder = OnePageParameterEncoder.class, action = {
        @AuthorizationAction(actionUri = PageSelf.AUTH_SELF_ALL_URI,
                label = PageSelf.AUTH_SELF_ALL_LABEL,
                description = PageSelf.AUTH_SELF_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SELF_REQUESTS_ASSIGNMENTS_URL,
                label = "PageAssignmentShoppingKart.auth.requestAssignments.label",
                description = "PageAssignmentShoppingKart.auth.requestAssignments.description")})
public class PageAssignmentsList<F extends FocusType> extends PageBase{
    private static final String ID_ASSIGNMENT_TABLE_PANEL = "assignmentTablePanel";
    private static final String ID_FORM = "mainForm";
    private static final String ID_BACK = "back";
    private static final String ID_REQUEST_BUTTON = "request";
    private static final String ID_RESOLVE_CONFLICTS_BUTTON = "resolveConflicts";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_TARGET_USER_PANEL = "targetUserPanel";

    private static final Trace LOGGER = TraceManager.getTrace(PageRequestRole.class);
    private static final String DOT_CLASS = PageAssignmentsList.class.getName() + ".";
    private static final String OPERATION_REQUEST_ASSIGNMENTS = DOT_CLASS + "requestAssignments";
    private static final String OPERATION_WF_TASK_CREATED = "com.evolveum.midpoint.wf.impl.WfHook.invoke";
    private static final String OPERATION_PREVIEW_ASSIGNMENT_CONFLICTS = "reviewAssignmentConflicts";

    private IModel<List<AssignmentEditorDto>> assignmentsModel;
    private OperationResult backgroundTaskOperationResult = null;
    IModel<String> descriptionModel;

    public PageAssignmentsList(){
        this(false);
    }

    public PageAssignmentsList(boolean loadConflicts){
        initModels();
        if (loadConflicts){
            getSessionStorage().getRoleCatalog().setConflictsList(getAssignmentConflicts());
        }
        initLayout();
    }

    private void initModels() {
        assignmentsModel = Model.ofList(getSessionStorage().getRoleCatalog().getAssignmentShoppingCart());
        descriptionModel = Model.of(getSessionStorage().getRoleCatalog().getRequestDescription());
    }

    public void initLayout() {
        setOutputMarkupId(true);

        Form mainForm = new Form(ID_FORM);
        mainForm.setOutputMarkupId(true);
        add(mainForm);

        AssignmentTablePanel panel = new AssignmentTablePanel<UserType>(ID_ASSIGNMENT_TABLE_PANEL,
                createStringResource("FocusType.assignment"), assignmentsModel, PageAssignmentsList.this){
            @Override
            protected List<InlineMenuItem> createAssignmentMenu() {
                List<InlineMenuItem> items = new ArrayList<>();
                InlineMenuItem item = new InlineMenuItem(createStringResource("AssignmentTablePanel.menu.unassign"),
                        new InlineMenuItemAction() {
                            private static final long serialVersionUID = 1L;

                            @Override
                            public void onClick(AjaxRequestTarget target) {
                                deleteAssignmentPerformed(target);
                            }
                        });
                items.add(item);
                return items;
            }
        };
        mainForm.add(panel);

        WebMarkupContainer targetUserPanel = new TargetUserSelectorComponent(ID_TARGET_USER_PANEL, PageAssignmentsList.this);
        targetUserPanel.setOutputMarkupId(true);
        mainForm.add(targetUserPanel);

        TextArea<String> descriptionInput = new TextArea<>(ID_DESCRIPTION, descriptionModel);
        descriptionInput.add(new AjaxFormComponentUpdatingBehavior("blur") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                getSessionStorage().getRoleCatalog().setRequestDescription(getDescriptionComponent().getValue());
            }
        });
        mainForm.add(descriptionInput);

        AjaxButton back = new AjaxButton(ID_BACK, createStringResource("PageAssignmentDetails.backButton")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                redirectBack();
            }

        };
        mainForm.add(back);

        AjaxSubmitButton requestAssignments = new AjaxSubmitButton(ID_REQUEST_BUTTON, createStringResource("PageAssignmentsList.requestButton")) {

            @Override
            protected void onError(AjaxRequestTarget target, org.apache.wicket.markup.html.form.Form<?> form) {
                target.add(getFeedbackPanel());
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target, org.apache.wicket.markup.html.form.Form<?> form) {
                if (getSessionStorage().getRoleCatalog().getTargetUserList() == null ||
                        getSessionStorage().getRoleCatalog().getTargetUserList().size() <= 1) {
                    onSingleUserRequestPerformed(target);
                } else {
                    onMultiUserRequestPerformed(target);
                }
            }

        };
        requestAssignments.add(new VisibleEnableBehaviour(){
            @Override
            public boolean isEnabled(){
                return getSessionStorage().getRoleCatalog().isMultiUserRequest() || areConflictsResolved();
            }
        });
        mainForm.add(requestAssignments);

        AjaxSubmitButton resolveAssignments = new AjaxSubmitButton(ID_RESOLVE_CONFLICTS_BUTTON,
                createStringResource("PageAssignmentsList.resolveConflicts")) {

            @Override
            protected void onError(AjaxRequestTarget target, org.apache.wicket.markup.html.form.Form<?> form) {
                target.add(getFeedbackPanel());
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target, org.apache.wicket.markup.html.form.Form<?> form) {
                PageAssignmentsList.this.navigateToNext(PageAssignmentConflicts.class);
            }

        };
        resolveAssignments.add(new VisibleEnableBehaviour(){
            @Override
            public boolean isVisible(){
                return !getSessionStorage().getRoleCatalog().isMultiUserRequest()
                        && !areConflictsResolved();
            }
        });
        mainForm.add(resolveAssignments);

    }

    private void onSingleUserRequestPerformed(AjaxRequestTarget target) {
        OperationResult result = new OperationResult(OPERATION_REQUEST_ASSIGNMENTS);
        ObjectDelta<UserType> delta;
        try {
            PrismObject<UserType> user = getTargetUser();
            delta = prepareDelta(user, result);

            getModelService().executeChanges(Collections.singletonList(delta), createOptions(), createSimpleTask(OPERATION_REQUEST_ASSIGNMENTS), result);

            result.recordSuccess();
            SessionStorage storage = getSessionStorage();
            storage.getRoleCatalog().getAssignmentShoppingCart().clear();
        } catch (Exception e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Could not save assignments ", e);
            error("Could not save assignments. Reason: " + e);
            target.add(getFeedbackPanel());
        } finally {
            result.recomputeStatus();
        }

        findBackgroundTaskOperation(result);
        if (backgroundTaskOperationResult != null
                && StringUtils.isNotEmpty(backgroundTaskOperationResult.getBackgroundTaskOid())){
            result.setMessage(createStringResource("operation.com.evolveum.midpoint.web.page.self.PageRequestRole.taskCreated").getString());
            showResult(result);
            setResponsePage(PageAssignmentShoppingKart.class);
            return;
        }
        showResult(result);
        if (!WebComponentUtil.isSuccessOrHandledError(result)) {
            target.add(getFeedbackPanel());
            target.add(PageAssignmentsList.this.get(ID_FORM));
        } else {
            setResponsePage(PageAssignmentShoppingKart.class);
        }
    }

    @NotNull
    private ModelExecuteOptions createOptions() {
        OperationBusinessContextType businessContextType;
        if (descriptionModel.getObject() != null) {
            businessContextType = new OperationBusinessContextType();
            businessContextType.setComment(descriptionModel.getObject());
        } else {
            businessContextType = null;
        }
        ModelExecuteOptions options = ExecuteChangeOptionsDto.createFromSystemConfiguration().createOptions();
        options.setRequestBusinessContext(businessContextType);
        return options;
    }

    private void onMultiUserRequestPerformed(AjaxRequestTarget target) {
            OperationResult result = new OperationResult(OPERATION_REQUEST_ASSIGNMENTS);
            Task operationalTask = createSimpleTask(OPERATION_REQUEST_ASSIGNMENTS);

            try {
                TaskType task = WebComponentUtil.createSingleRecurrenceTask(
                        createStringResource(OPERATION_REQUEST_ASSIGNMENTS).getString(),
                        UserType.COMPLEX_TYPE,
                        getTaskQuery(), prepareDelta(null, result), createOptions(), TaskCategory.EXECUTE_CHANGES, PageAssignmentsList.this);
                WebModelServiceUtils.runTask(task, operationalTask, result, PageAssignmentsList.this);
            } catch (SchemaException e) {
                result.recordFatalError(result.getOperation(), e);
                LoggingUtils.logUnexpectedException(LOGGER,
                        "Failed to execute operaton " + result.getOperation(), e);
                target.add(getFeedbackPanel());
            }
            findBackgroundTaskOperation(result);
            if (backgroundTaskOperationResult != null
                    && StringUtils.isNotEmpty(backgroundTaskOperationResult.getBackgroundTaskOid())) {
                result.setMessage(createStringResource("operation.com.evolveum.midpoint.web.page.self.PageRequestRole.taskCreated").getString());
                showResult(result);
                setResponsePage(PageAssignmentShoppingKart.class);
                return;
            }
            if (WebComponentUtil.isSuccessOrHandledError(result)
                    || OperationResultStatus.IN_PROGRESS.equals(result.getStatus())) {
                SessionStorage storage = getSessionStorage();
                if (storage.getRoleCatalog().getAssignmentShoppingCart() != null) {
                    storage.getRoleCatalog().getAssignmentShoppingCart().clear();
                }
                if (storage.getRoleCatalog().getTargetUserList() != null){
                    storage.getRoleCatalog().getTargetUserList().clear();
                }
                setResponsePage(PageAssignmentShoppingKart.class);
            } else {
                showResult(result);
                target.add(getFeedbackPanel());
                target.add(PageAssignmentsList.this.get(ID_FORM));
            }
    }

    private ContainerDelta handleAssignmentDeltas(ObjectDelta<UserType> focusDelta,
                                                  List<AssignmentEditorDto> assignments, PrismContainerDefinition def) throws SchemaException {
        ContainerDelta assDelta = new ContainerDelta(new ItemPath(), def.getName(), def, getPrismContext());

        for (AssignmentEditorDto assDto : assignments) {
            PrismContainerValue newValue = assDto.getNewValue(getPrismContext());

            switch (assDto.getStatus()) {
                case ADD:
                    newValue.applyDefinition(def, false);
                    assDelta.addValueToAdd(newValue.clone());
                    break;
                case DELETE:
                    PrismContainerValue oldValue = assDto.getOldValue();
                    oldValue.applyDefinition(def);
                    assDelta.addValueToDelete(oldValue.clone());
                    break;
                case MODIFY:
                    if (!assDto.isModified(getPrismContext())) {
                        LOGGER.trace("Assignment '{}' not modified.", new Object[]{assDto.getName()});
                        continue;
                    }

                    handleModifyAssignmentDelta(assDto, def, newValue, focusDelta);
                    break;
                default:
                    warn(getString("pageAdminUser.message.illegalAssignmentState", assDto.getStatus()));
            }
        }

        if (!assDelta.isEmpty()) {
            assDelta = focusDelta.addModification(assDelta);
        }

        return assDelta;
    }


    private void findBackgroundTaskOperation(OperationResult result){
        if (backgroundTaskOperationResult != null) {
            return;
        } else {
            List<OperationResult> subresults = result.getSubresults();
            if (subresults == null || subresults.size() == 0) {
                return;
            }
            for (OperationResult subresult : subresults) {
                if (subresult.getOperation().equals(OPERATION_WF_TASK_CREATED)) {
                    backgroundTaskOperationResult = subresult;
                    return;
                } else {
                    findBackgroundTaskOperation(subresult);
                }
            }
        }
        return;
    }

    private void handleModifyAssignmentDelta(AssignmentEditorDto assDto,
                                             PrismContainerDefinition assignmentDef, PrismContainerValue newValue, ObjectDelta<UserType> focusDelta)
            throws SchemaException {
        LOGGER.debug("Handling modified assignment '{}', computing delta.",
                new Object[]{assDto.getName()});

        PrismValue oldValue = assDto.getOldValue();
        Collection<? extends ItemDelta> deltas = oldValue.diff(newValue);

        for (ItemDelta delta : deltas) {
            ItemPath deltaPath = delta.getPath().rest();
            ItemDefinition deltaDef = assignmentDef.findItemDefinition(deltaPath);

            delta.setParentPath(WebComponentUtil.joinPath(oldValue.getPath(), delta.getPath().allExceptLast()));
            delta.applyDefinition(deltaDef);

            focusDelta.addModification(delta);
        }
    }

    private List<AssignmentConflictDto> getAssignmentConflicts(){
        ModelContext<UserType> modelContext = null;

        ObjectDelta<UserType> delta;
        OperationResult result = new OperationResult(OPERATION_PREVIEW_ASSIGNMENT_CONFLICTS);
        Task task = createSimpleTask(OPERATION_PREVIEW_ASSIGNMENT_CONFLICTS);
        List<AssignmentConflictDto> conflictsList = new ArrayList<>();
        try {
            PrismObject<UserType> user = getTargetUser();
            delta = user.createModifyDelta();

            PrismContainerDefinition def = user.getDefinition().findContainerDefinition(UserType.F_ASSIGNMENT);
            handleAssignmentDeltas(delta, getSessionStorage().getRoleCatalog().getAssignmentShoppingCart(), def);

            PartialProcessingOptionsType partialProcessing = new PartialProcessingOptionsType();
            partialProcessing.setInbound(PartialProcessingTypeType.SKIP);
            partialProcessing.setProjection(PartialProcessingTypeType.SKIP);
			ModelExecuteOptions recomputeOptions = ModelExecuteOptions.createPartialProcessing(partialProcessing);
			modelContext = getModelInteractionService()
                    .previewChanges(WebComponentUtil.createDeltaCollection(delta), recomputeOptions, task, result);
            DeltaSetTriple<? extends EvaluatedAssignment> evaluatedAssignmentTriple = modelContext
                    .getEvaluatedAssignmentTriple();
            Collection<? extends EvaluatedAssignment> addedAssignments = evaluatedAssignmentTriple
                    .getPlusSet();
            Map<String, AssignmentConflictDto> conflictOidsMap = new HashMap<>();
            if (addedAssignments != null) {
                for (EvaluatedAssignment<UserType> evaluatedAssignment : addedAssignments) {
                    for (EvaluatedPolicyRule policyRule : evaluatedAssignment.getAllTargetsPolicyRules()) {
                        for (EvaluatedPolicyRuleTrigger<?> trigger : policyRule.getAllTriggers()) {
                            if (trigger instanceof EvaluatedExclusionTrigger) {
                                EvaluatedExclusionTrigger exclusionTrigger = (EvaluatedExclusionTrigger) trigger;
                                EvaluatedAssignment<F> conflictingAssignment = exclusionTrigger.getConflictingAssignment();
                                if (conflictingAssignment.getAssignmentType(true) == null) {
                                    LOGGER.trace("The conflicting assignment is newly added: {}, for evaluated assignment {}",
                                            conflictingAssignment, evaluatedAssignment);
                                } else {
                                    LOGGER.trace("The conflicting assignment is NOT newly added: {}, for evaluated assignment {}",
                                            conflictingAssignment, evaluatedAssignment);
                                }
                                PrismObject<F> addedAssignmentTargetObj = (PrismObject<F>)evaluatedAssignment.getTarget();
                                PrismObject<F> exclusionTargetObj = (PrismObject<F>)conflictingAssignment.getTarget();
                                AssignmentConflictDto dto = new AssignmentConflictDto(exclusionTargetObj, addedAssignmentTargetObj);
                                boolean isWarning = policyRule.getActions() != null
                                        && policyRule.getActions().getApproval() != null;
                                    dto.setError(!isWarning);
                                if (conflictOidsMap.containsKey(exclusionTargetObj.getOid()) && isWarning){
                                    conflictOidsMap.replace(exclusionTargetObj.getOid(), dto);
                                } else {
                                    conflictOidsMap.put(exclusionTargetObj.getOid(), dto);
                                }
                            }
                        }

                    }
                }
            }
            conflictsList.addAll(conflictOidsMap.values());
        } catch (Exception e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't get assignments conflicts. Reason: ", e);
            error("Couldn't get assignments conflicts. Reason: " + e);
        }
        return conflictsList;
    }

    private boolean areConflictsResolved(){
        List<AssignmentConflictDto> list = getSessionStorage().getRoleCatalog().getConflictsList();
        for (AssignmentConflictDto dto : list){
            if (!dto.isError()){
                continue;
            }
            if (!dto.isSolved()){
                return false;
            }
        }
        return true;
    }

    private ObjectDelta prepareDelta(PrismObject<UserType> user, OperationResult result) {
        ObjectDelta delta = null;
        try{
            delta = ObjectDelta.createModificationAddContainer(UserType.class, user == null ? "fakeOid" : user.getOid(),
                FocusType.F_ASSIGNMENT, getPrismContext(), getAddAssignmentContainerValues(assignmentsModel.getObject()));
            if (!getSessionStorage().getRoleCatalog().isMultiUserRequest()) {
                delta.addModificationDeleteContainer(FocusType.F_ASSIGNMENT,
                        getDeleteAssignmentContainerValues(user.asObjectable()));
            }
        } catch (SchemaException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Failed to prepare delta for operation " + OPERATION_REQUEST_ASSIGNMENTS, e);
            result.recordFatalError("Failed to prepare delta for operation " + OPERATION_REQUEST_ASSIGNMENTS, e);
        }
        return delta;

    }

    private ObjectQuery getTaskQuery(){
        List<PrismObject<UserType>> userList = getSessionStorage().getRoleCatalog().getTargetUserList();
        if (getSessionStorage().getRoleCatalog().isSelfRequest()){
            userList = new ArrayList<>();
            userList.add(loadUserSelf(PageAssignmentsList.this));
        }
        Set<String> oids = new HashSet<>();
        for (PrismObject<UserType> user : userList){
            oids.add(user.getOid());
        }
        return ObjectQuery.createObjectQuery(InOidFilter.createInOid(oids));
    }

    private PrismContainerValue[] getAddAssignmentContainerValues(List<AssignmentEditorDto> assignments) throws SchemaException {

        List<PrismContainerValue<AssignmentType>> addContainerValues = new ArrayList<>();
        for (AssignmentEditorDto assDto : assignments) {
            if (UserDtoStatus.ADD.equals(assDto.getStatus())) {
                addContainerValues.add(assDto.getNewValue(getPrismContext()).clone());      // clone is to eliminate "Attempt to reset value parent ..." exceptions (in some cases)
            }

        }
        return addContainerValues.toArray(new PrismContainerValue[addContainerValues.size()]);
    }

    private PrismContainerValue[] getDeleteAssignmentContainerValues(UserType user) throws SchemaException {
        List<PrismContainerValue<AssignmentType>> deleteAssignmentValues = new ArrayList<>();
        for (AssignmentEditorDto assDto : getAssignmentsToRemoveList(user)) {
            deleteAssignmentValues.add(assDto.getNewValue(getPrismContext()));
        }
        return deleteAssignmentValues.toArray(new PrismContainerValue[deleteAssignmentValues.size()]);
    }

    private List<AssignmentEditorDto> getAssignmentsToRemoveList(UserType user){
        List<AssignmentConflictDto> conflicts = getSessionStorage().getRoleCatalog().getConflictsList();
        List<String> assignmentsToRemoveOids = new ArrayList<>();
        for (AssignmentConflictDto dto : conflicts){
            if (dto.isRemovedOld()){
                assignmentsToRemoveOids.add(dto.getExistingAssignmentTargetObj().getOid());
            }
        }

        List<AssignmentEditorDto> assignmentsToDelete = new ArrayList<>();
        for (AssignmentType assignment : user.getAssignment()){
            if (assignment.getTargetRef() == null){
                continue;
            }
            if (assignmentsToRemoveOids.contains(assignment.getTargetRef().getOid())){
                assignmentsToDelete.add(new AssignmentEditorDto(UserDtoStatus.DELETE, assignment, this));
            }
        }
        return assignmentsToDelete;
    }

    private TextArea getDescriptionComponent(){
        return (TextArea) get(ID_FORM).get(ID_DESCRIPTION);
    }

    private PrismObject<UserType> getTargetUser() throws SchemaException {
        List<PrismObject<UserType>> usersList = getSessionStorage().getRoleCatalog().getTargetUserList();
        PrismObject<UserType> user = getSessionStorage().getRoleCatalog().isSelfRequest() ?
                loadUserSelf(PageAssignmentsList.this) : usersList.get(0);
        getPrismContext().adopt(user);
        return user;
    }

    @Override
    public boolean canRedirectBack(){
        return true;
    }

}
