package org.coldis.library.persistence.batch;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.reflect.MethodUtils;
import org.coldis.library.exception.BusinessException;
import org.coldis.library.exception.IntegrationException;
import org.coldis.library.helper.DateTimeHelper;
import org.coldis.library.model.SimpleMessage;
import org.coldis.library.model.Typable;
import org.coldis.library.model.view.ModelView;
import org.coldis.library.persistence.bean.StaticContextAccessor;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;

/**
 * Batch executor.
 */
@JsonTypeName(value = BatchExecutor.TYPE_NAME)
public class BatchExecutor implements Typable {

	/**
	 * Serial.
	 */
	private static final long serialVersionUID = 3022111202119271553L;

	/**
	 * Type name.
	 */
	public static final String TYPE_NAME = "org.coldis.library.persistence.batch.BatchExecutor";

	/**
	 * Key suffix.
	 */
	private String keySuffix;

	/**
	 * Batch size.
	 */
	private Long size = 13000L;

	/**
	 * Last processed id.
	 */
	private String lastProcessedId;

	/**
	 * Maximum interval to finish the batch.
	 */
	private Duration finishWithin = Duration.ofDays(1);

	/**
	 * Arguments used to get next batch.
	 */
	private Map<String, String> arguments;

	/**
	 * Action bean name.
	 */
	private String actionBeanName;

	/**
	 * Action delegate methods.
	 */
	private Map<BatchAction, String> actionDelegateMethods;

	/**
	 * Messages templates.
	 */
	private Map<BatchAction, String> messagesTemplates;

	/**
	 * Slack channels to communicate.
	 */
	private Map<BatchAction, String> slackChannels;

	/**
	 * No arguments constructor.
	 */
	public BatchExecutor() {
		super();
	}

	/**
	 * Default constructor.
	 *
	 * @param keySuffix       Key suffix.
	 * @param size            Size.
	 * @param lastProcessedId Last processed id.
	 * @param finishWithin    Maximum interval to finish the batch.
	 */
	public BatchExecutor(final String keySuffix, final Long size, final String lastProcessedId, final Duration finishWithin) {
		super();
		this.keySuffix = keySuffix;
		this.size = size;
		this.lastProcessedId = lastProcessedId;
		this.finishWithin = finishWithin;
	}

	/**
	 * Complete constructor.
	 *
	 * @param keySuffix             Key suffix.
	 * @param size                  Size.
	 * @param lastProcessedId       Last processed id.
	 * @param finishWithin          Maximum interval to finish the batch.
	 * @param actionBeanName        Action bean name.
	 * @param actionDelegateMethods Action delegate methods.
	 * @param messagesTemplates     Messages templates.
	 * @param slackChannels         Slack channels.
	 */
	public BatchExecutor(
			final String keySuffix,
			final Long size,
			final String lastProcessedId,
			final Duration finishWithin,
			final String actionBeanName,
			final Map<BatchAction, String> actionDelegateMethods,
			final Map<BatchAction, String> messagesTemplates,
			final Map<BatchAction, String> slackChannels) {
		super();
		this.keySuffix = keySuffix;
		this.size = size;
		this.lastProcessedId = lastProcessedId;
		this.finishWithin = finishWithin;
		this.actionBeanName = actionBeanName;
		this.actionDelegateMethods = actionDelegateMethods;
		this.messagesTemplates = messagesTemplates;
		this.slackChannels = slackChannels;
	}

	/**
	 * Gets the keySuffix.
	 *
	 * @return The keySuffix.
	 */
	public String getKeySuffix() {
		return this.keySuffix;
	}

	/**
	 * Sets the keySuffix.
	 *
	 * @param keySuffix New keySuffix.
	 */
	public void setKeySuffix(
			final String keySuffix) {
		this.keySuffix = keySuffix;
	}

	/**
	 * Gets the size.
	 *
	 * @return The size.
	 */
	public Long getSize() {
		return this.size;
	}

	/**
	 * Sets the size.
	 *
	 * @param size New size.
	 */
	public void setSize(
			final Long size) {
		this.size = size;
	}

	/**
	 * Gets the lastProcessedId.
	 *
	 * @return The lastProcessedId.
	 */
	public String getLastProcessedId() {
		return this.lastProcessedId;
	}

	/**
	 * Sets the lastProcessedId.
	 *
	 * @param lastProcessedId New lastProcessedId.
	 */
	public void setLastProcessedId(
			final String lastProcessedId) {
		this.lastProcessedId = lastProcessedId;
	}

	/**
	 * Gets the finishWithin.
	 *
	 * @return The finishWithin.
	 */
	public Duration getFinishWithin() {
		return this.finishWithin;
	}

	/**
	 * Sets the finishWithin.
	 *
	 * @param finishWithin New finishWithin.
	 */
	public void setFinishWithin(
			final Duration finishWithin) {
		this.finishWithin = finishWithin;
	}

	/**
	 * Gets the finishWithin.
	 *
	 * @return The finishWithin.
	 */
	public LocalDateTime getExpiration() {
		return (this.finishWithin == null ? null : DateTimeHelper.getCurrentLocalDateTime().minus(this.getFinishWithin()));
	}

	/**
	 * Gets the arguments.
	 *
	 * @return The arguments.
	 */
	public Map<String, String> getArguments() {
		this.arguments = (this.arguments == null ? new HashMap<>() : this.arguments);
		return this.arguments;
	}

	/**
	 * Sets the arguments.
	 *
	 * @param arguments New arguments.
	 */
	public void setGetArguments(
			final Map<String, String> getArguments) {
		this.arguments = getArguments;
	}

	/**
	 * Gets the actionBeanName.
	 *
	 * @return The actionBeanName.
	 */
	public String getActionBeanName() {
		return this.actionBeanName;
	}

	/**
	 * Sets the actionBeanName.
	 *
	 * @param actionBeanName New actionBeanName.
	 */
	public void setActionBeanName(
			final String actionBeanName) {
		this.actionBeanName = actionBeanName;
	}

	/**
	 * Gets the actionDelegateMethods.
	 *
	 * @return The actionDelegateMethods.
	 */
	public Map<BatchAction, String> getActionDelegateMethods() {
		this.actionDelegateMethods = (this.actionDelegateMethods == null
				? new HashMap<>(Map.of(BatchAction.START, "start", BatchAction.RESUME, "resume", BatchAction.GET, "get", BatchAction.EXECUTE, "execute",
						BatchAction.FINISH, "finish"))
				: this.actionDelegateMethods);
		return this.actionDelegateMethods;
	}

	/**
	 * Sets the actionDelegateMethods.
	 *
	 * @param actionDelegateMethods New actionDelegateMethods.
	 */
	public void setActionDelegateMethods(
			final Map<BatchAction, String> actionDelegateMethods) {
		this.actionDelegateMethods = actionDelegateMethods;
	}

	/**
	 * Executes a delegate method.
	 *
	 * @param  action            Action.
	 * @param  arguments         Arguments.
	 * @return                   The object return.
	 * @throws BusinessException If the method fails.
	 */
	public Object executeActionDelegateMethod(
			final BatchAction action,
			final Object... arguments) throws BusinessException {
		Object returnObject = null;
		if (this.getActionBeanName() != null) {
			if (this.getActionDelegateMethods().containsKey(action)) {
				try {
					final Object bean = StaticContextAccessor.getBean(this.getActionBeanName());
					returnObject = MethodUtils.invokeMethod(bean, this.getActionDelegateMethods().get(action), arguments);
				}
				catch (final IntegrationException exception) {
					throw exception;
				}
				catch (final Exception exception) {
					throw new IntegrationException(new SimpleMessage("batch.action.error"), exception);
				}
			}
		}
		return returnObject;
	}

	/**
	 * Gets the messagesTemplates.
	 *
	 * @return The messagesTemplates.
	 */
	public Map<BatchAction, String> getMessagesTemplates() {
		this.messagesTemplates = (this.messagesTemplates == null ? new HashMap<>(Map.of(BatchAction.START, "Starting batch for '${key}'.", BatchAction.RESUME,
				"Resuming batch for '${key}' from id '${id}'.", BatchAction.FINISH, "Finishing batch for '${key}' at id '${id}' in '${duration}' minutes."))
				: this.messagesTemplates);
		return this.messagesTemplates;
	}

	/**
	 * Sets the messagesTemplates.
	 *
	 * @param messagesTemplates New messagesTemplates.
	 */
	public void setMessagesTemplates(
			final Map<BatchAction, String> messagesTemplates) {
		this.messagesTemplates = messagesTemplates;
	}

	/**
	 * Gets the slackChannels.
	 *
	 * @return The slackChannels.
	 */
	public Map<BatchAction, String> getSlackChannels() {
		this.slackChannels = (this.slackChannels == null ? new HashMap<>() : this.slackChannels);
		return this.slackChannels;
	}

	/**
	 * Sets the slackChannels.
	 *
	 * @param slackChannels New slackChannels.
	 */
	public void setSlackChannels(
			final Map<BatchAction, String> slackChannels) {
		this.slackChannels = slackChannels;
	}

	/**
	 * @see org.coldis.library.model.Typable#getTypeName()
	 */
	@Override
	@JsonView({ ModelView.Persistent.class, ModelView.Public.class })
	public String getTypeName() {
		return BatchExecutor.TYPE_NAME;
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return Objects.hash(actionBeanName, actionDelegateMethods, arguments, finishWithin, keySuffix, lastProcessedId, messagesTemplates, size, slackChannels);
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(
			Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof BatchExecutor)) {
			return false;
		}
		BatchExecutor other = (BatchExecutor) obj;
		return Objects.equals(actionBeanName, other.actionBeanName) && Objects.equals(actionDelegateMethods, other.actionDelegateMethods)
				&& Objects.equals(arguments, other.arguments) && Objects.equals(finishWithin, other.finishWithin) && Objects.equals(keySuffix, other.keySuffix)
				&& Objects.equals(lastProcessedId, other.lastProcessedId) && Objects.equals(messagesTemplates, other.messagesTemplates)
				&& Objects.equals(size, other.size) && Objects.equals(slackChannels, other.slackChannels);
	}

	/**
	 * Starts the batch.
	 *
	 * @throws BusinessException If start fails.
	 */
	public void start() throws BusinessException {
		this.executeActionDelegateMethod(BatchAction.START);
	}

	/**
	 * Resumes the batch.
	 *
	 * @throws BusinessException If resume fails.
	 */
	public void resume() throws BusinessException {
		this.executeActionDelegateMethod(BatchAction.RESUME);
	}

	/**
	 * Gets the next batch to be processed.
	 *
	 * @return                   The next batch to be processed.
	 * @throws BusinessException If the next to process cannot be retrieved.
	 */
	@SuppressWarnings("unchecked")
	public List<String> get() throws BusinessException {
		return (List<String>) this.executeActionDelegateMethod(BatchAction.GET, this.getLastProcessedId(), this.getSize(), this.getArguments());
	}

	/**
	 * Finishes the batch.
	 *
	 * @throws BusinessException If finish fails.
	 */
	public void finish() throws BusinessException {
		this.executeActionDelegateMethod(BatchAction.FINISH);
	}

	/**
	 * Executes the batch for one item.
	 *
	 * @param  id                Item id.
	 * @throws BusinessException If execution fails.
	 */
	public void execute(
			final String id) throws BusinessException {
		this.executeActionDelegateMethod(BatchAction.EXECUTE, id);
	}

}
