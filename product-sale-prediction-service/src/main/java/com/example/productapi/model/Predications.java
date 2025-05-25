package com.example.productapi.model;

import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author cicidi on 5/23/25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Predications {
  private List<Predication> predicationList;
  private LocalDate startDate;
  private LocalDate endDate;
  private int totalQuantity;
  private int totalDays;
}
