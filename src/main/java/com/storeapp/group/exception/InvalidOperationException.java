package com.storeapp.group.exception;

import com.storeapp.shared.exception.BusinessException;

/**
 * Eccezione lanciata quando si tenta un'operazione non permessa
 */
public class InvalidOperationException extends BusinessException {

    public InvalidOperationException(String message) {
        super(message, "INVALID_OPERATION", 400);
    }
    
    public static InvalidOperationException lastAdminCannotLeave() {
        return new InvalidOperationException(
            "L'ultimo amministratore non può abbandonare il gruppo. " +
            "Nomina prima un altro amministratore o elimina il gruppo.");
    }
    
    public static InvalidOperationException cannotRemoveYourself() {
        return new InvalidOperationException("Non puoi rimuovere te stesso dal gruppo. Usa la funzione 'Abbandona gruppo'.");
    }
    
    public static InvalidOperationException maxMembersReached() {
        return new InvalidOperationException("Il gruppo ha raggiunto il numero massimo di membri (50)");
    }

    public static InvalidOperationException memberInvolvedInActivities(String memberName, long activityCount, long expenseCount) {
        StringBuilder msg = new StringBuilder();
        msg.append("Impossibile rimuovere ").append(memberName).append(": ");
        if (activityCount > 0 && expenseCount > 0) {
            msg.append("è coinvolto in ").append(activityCount)
               .append(" attivit").append(activityCount == 1 ? "à" : "à")
               .append(" e ").append(expenseCount)
               .append(" spes").append(expenseCount == 1 ? "a" : "e").append(".");
        } else if (activityCount > 0) {
            msg.append("è coinvolto in ").append(activityCount)
               .append(" attivit").append(activityCount == 1 ? "à" : "à").append(".");
        } else {
            msg.append("è coinvolto in ").append(expenseCount)
               .append(" spes").append(expenseCount == 1 ? "a" : "e").append(".");
        }
        msg.append(" Rimuovilo prima dalle attività e dalle spese.");
        return new InvalidOperationException(msg.toString());
    }
}
