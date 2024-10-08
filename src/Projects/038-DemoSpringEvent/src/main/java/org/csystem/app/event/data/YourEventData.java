package org.csystem.app.event.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
public class YourEventData {
    @Accessors(prefix = "m_")
    private String m_username;

    @Accessors(prefix = "m_")
    private int m_count;

    @Accessors(prefix = "m_")
    private String m_numbers;

    public YourEventData(String username, int count)
    {
        m_username = username;
        m_count = count;
    }
}
