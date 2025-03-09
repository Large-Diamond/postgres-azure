import { Component, computed, inject } from '@angular/core';
import { BackupsService } from '../backups/backups.service';
import { TablesService } from './tables.service';

@Component({
  selector: 'app-tables',
  standalone: true,
  templateUrl: './tables.component.html',
  styleUrl: './tables.component.css',
})
export class TablesComponent {
  private readonly tabeService = inject(TablesService);
  private readonly backupsService = inject(BackupsService);
  readonly tableData = this.tabeService.tables;
  tables = computed(() => this.tableData.value()?.tables);
  totalRowCount = computed(
    () => this.tableData.value()?.totalRowCount
  );
  processing = computed(
    () => this.tabeService.processing() || this.backupsService.processing()
  );
}
