package com.example.productapi.model;

import java.time.LocalDate;
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
public class Predication {
  private LocalDate date;
  private int quantity;
}
