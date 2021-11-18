package com.example.streams.conversations;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataRecord {

	private Long id;
	private Long timeRead;
	private Long from;
	private Long to;
	private Long workspaceId;
	private Long timestamp;
	private Long timeSent;
	private String eventType;
	private Long orderTotal;
	private String productName;

	   public String toString() {
        return new com.google.gson.Gson().toJson(this);
    }

}
